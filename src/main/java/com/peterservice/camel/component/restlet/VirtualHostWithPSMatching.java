package com.peterservice.camel.component.restlet;

import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.routing.Route;
import org.restlet.routing.Template;
import org.restlet.routing.VirtualHost;

import java.util.*;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * User: Alexey.Borodin
 * Date: 25.05.15
 * Time: 14:56
 * To change this template use File | Settings | File Templates.
 */
public class VirtualHostWithPSMatching extends VirtualHost {

    private static Pattern pattern = Pattern.compile("((?:[a-zA-Z\\d\\-\\.\\_\\~\\!\\$\\&\\'\\(\\)\\*\\+\\,\\;\\=\\:\\@]|(?:\\%[\\dABCDEFabcdef][\\dABCDEFabcdef]))+)");
    private List<RouteSplit<Route, String[]>> splittedRoutes = new LinkedList<RouteSplit<Route, String[]>>();
    private volatile boolean isRoutesUpToDate;
    private static List<Integer> constIndexes = new ArrayList<Integer>(2);

    static {
        constIndexes.add(0);
        constIndexes.add(0);
    }

    private static class RouteSplit<K, V> implements Map.Entry<K, V> //Вспомогательный класс для хранения пары "Маршрут - Массив частей URL"
    {
        private K key;
        private V value;

        public RouteSplit(K key, V value) {
            this.key = key;
            this.value = value;
        }
        @Override
        public K getKey() {
            return key;
        }
        @Override
        public V getValue() {
            return value;
        }
        @Override
        public V setValue(V value) {
            V old = this.value;
            this.value = value;
            return old;
        }
    }
   
    public VirtualHostWithPSMatching(VirtualHost copyFrom) {
        super(copyFrom.getContext(), ".*", ".*", ".*", ".*", ".*", ".*", ".*", ".*");
        init();
        // непонятно, для чего происходит вызов
        // этих методов
        setDefaultMatchingMode(Template.MODE_EQUALS);
        setRoutingMode(MODE_CUSTOM);
    }

    private synchronized void init() {
        splittedRoutes.clear();
        for (final Route current : getRoutes()) {
            if (current.getTemplate() != null) {
                String localPattern = current.getTemplate().getPattern();
                if (localPattern!=null && localPattern.startsWith("/")) {
                    if (localPattern.length()>1) {
                        localPattern = localPattern.substring(1);
                    } else {
                        localPattern = "";
                    }
                }
                splittedRoutes.add(new RouteSplit<Route, String[]>(current, localPattern.split("/")));
            }
        }
        isRoutesUpToDate = true;
    }

    @Override
    public Route attach(String uriPattern, Restlet target) {
        isRoutesUpToDate = false;
        return super.attach(uriPattern, target);
    }

    @Override
    public void detach(Restlet target) {
        isRoutesUpToDate = false;
        super.detach(target);
    }

    // original version
    // deprecated due to optimizations
    // left only for performance tests
    @Deprecated
    protected Route getOriginalCustom(Request request, Response response) { //Не менялось и не использовалось
        float score;

        LinkedList<Route> matchingRoutes = new LinkedList<Route>();

        for (final Route current : getRoutes()) {
            score = score(current, request, response);

            if (score >= 1.0F) { // TODO
                matchingRoutes.add(current);
            }
        }

        if (matchingRoutes.isEmpty())
            return null;

        Route bestRoute = matchingRoutes.get(0);

        for (Route r : matchingRoutes) {
            if (r.equals(bestRoute)) {
                continue;
            }
            if (r.getTemplate() != null && r.getTemplate().getPattern() != null &&
                    secondUrlMoreSpecific(bestRoute.getTemplate().getPattern(), r.getTemplate().getPattern())) {
                bestRoute = r;
            }
        }

        return bestRoute;
    }

    @Override
    protected Route getCustom(Request request, Response response) {
        if (!isRoutesUpToDate) //Если маршруты были изменены, обновляем
        {
            init();
        }

        Object bestMatch = null;
        RouteSplit matcherAndSplittedRequest = requestParse(request); //Получаем матчеры и разделенную строку
        if (matcherAndSplittedRequest != null) {
            Matcher[] matchers = (Matcher[]) matcherAndSplittedRequest.getKey();
            String[] splittedRequest = (String[]) matcherAndSplittedRequest.getValue();
            List<Integer> varIndexes = constIndexes; //Для хранения позиций переменных url лучшего совпадения bestMatch
            for (final RouteSplit current : splittedRoutes) {
                List<Integer> result = match((String[]) current.getValue(), splittedRequest, matchers); //Получаем список индексов переменных или null, если совпадения нет
                if (result != null) {
                    ListIterator<Integer> varIndIterator = varIndexes.listIterator();
                    ListIterator<Integer> resultIterator = result.listIterator();
                    while (varIndIterator.hasNext() && resultIterator.hasNext()) {
                        int varIndValue = varIndIterator.next();
                        int resultValue = resultIterator.next();
                        if (varIndValue < resultValue) { //У current переменная url появляется позже, значить current более специфичен, чем bestMatch
                            bestMatch = current.getKey();
                            varIndexes = result;
                            break;
                        }
                    }
                    if (!resultIterator.hasNext() && varIndIterator.hasNext()) { //current более специфичен, т.к. содержит меньше переменных
                        bestMatch = current.getKey();
                        varIndexes = result;
                    }
                }
            }
        }
        return (Route) bestMatch;
    }

    protected RouteSplit requestParse(Request request) {
        if (request.getResourceRef() != null) {
            String remainingPart = request.getResourceRef()
                    .getRemainingPart(false, false); //Получаем url из запроса
            if ((remainingPart != null) && !remainingPart.isEmpty()) {
                if (remainingPart.charAt(0) == '/') {
                    if (remainingPart.length()>1) {
                        remainingPart = remainingPart.substring(1);
                    } else {
                        remainingPart = "";
                    }
                }
                String[] splittedRequest = remainingPart.split("/");
                Matcher[] matchers = new Matcher[splittedRequest.length]; //Для каждой части url создаем матчер
                for (int i = 0; i < splittedRequest.length; i++) {
                    matchers[i] = pattern.matcher(splittedRequest[i]);
                }
                return new RouteSplit(matchers, splittedRequest);
            }
        }
        return null;
    }

    @Deprecated
    private static String getUrlCommonPrefix(String url1, String url2) {//Не менялось и не использовалось
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < url1.length(); i++) {
            if (i >= url2.length())
                return sb.toString();

            if (url1.charAt(i) == url2.charAt(i))
                sb.append(url1.charAt(i));
            else
                return sb.toString();
        }

        return sb.toString();
    }

    @Deprecated
    private static boolean secondUrlMoreSpecific(String url1, String url2) {//Не менялось и не использовалось
        String requiredUrl1 = url1;
        String requiredUrl2 = url2;
        if (url1.startsWith("/")) {
            requiredUrl1 = requiredUrl1.substring(1);
        }
        if (url2.startsWith("/")) {
            requiredUrl2 = requiredUrl2.substring(1);
        }
        
        String commonPart = getUrlCommonPrefix(requiredUrl1, requiredUrl2);

        // common prefix but different second parts
        char url1next = requiredUrl1.charAt(commonPart.length());

        // url1 is less specific => url2 is more specific
        // or if url2 starts with url1 then url2 is more specific
        if ((url1next == '{') || (commonPart.length() == requiredUrl1.length()))
            return true;

        // don't swap if there's nothing in common with two functions
        if ((commonPart.isEmpty()) || (commonPart.length() == requiredUrl2.length()))
            return false;

        return false;
    }

    @Deprecated
    protected float score(Route current, Request request, Response response) {//Не менялось и не использовалось
        float result = 0F;

        if (request.getResourceRef() != null && current.getTemplate() != null) {
            final String remainingPart = request.getResourceRef()
                    .getRemainingPart(false, current.isMatchingQuery());
            if (remainingPart != null) {
                final int matchedLength = current.getTemplate().match(remainingPart);

                if (matchedLength != -1) {
                    final float totalLength = remainingPart.length();

                    if (totalLength > 0.0F) {
                        result = getRequiredScore()
                                + (1.0F - getRequiredScore())
                                * (matchedLength / totalLength);
                        if (current.getTemplate().getPattern().equals(remainingPart)) {
                            result += 0.1F;
                        }

                    } else {
                        result = 1.0F;
                    }
                }
            }

            if (getLogger().isLoggable(Level.FINER)) {
                getLogger().finer(
                        "Call score for the \"" + current.getTemplate().getPattern()
                                + "\" URI pattern: " + result);
            }
        }

        return result;
    }

    protected List<Integer> match(String[] splittedTemplate, String[] splittedRequest, Matcher[] matchers) {
        
        if (splittedTemplate.length != splittedRequest.length) //Если кол-во частей в url разное, то запрос под шаблон не подходит
            return null;
        List<Integer> varIndexes = new LinkedList<Integer>(); //Храним номера частей url, в которых содержатся переменные
        for (int i = 0; i < splittedTemplate.length; i++) {
            if (!splittedTemplate[i].equals(splittedRequest[i])) {
                if ((splittedTemplate[i].charAt(0) == '{') && (splittedTemplate[i].charAt(splittedTemplate[i].length() - 1) == '}')) { //В случае несовпадения проверяем, является ли эта часть переменной
                    if (!splittedRequest[i].isEmpty() && (!matchers[i].matches() || splittedRequest[i].contains("%7B") || splittedRequest[i].contains("%7D"))) //Чтобы не декодировать remainingPart в score3 ищем { и } в их ASCII виде
                        return null;
                    else {
                        varIndexes.add(i);
                    }
                } else {
                    return null;
                }
            }
        }
        return varIndexes;
    }
}
