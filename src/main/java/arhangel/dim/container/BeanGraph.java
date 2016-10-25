package arhangel.dim.container;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;


/**
 *
 */
public class BeanGraph {
    // Граф представлен в виде списка связности для каждой вершины
    private Map<BeanVertex, List<BeanVertex>> vertices = new HashMap<>();

    /**
     * Добавить вершину в граф
     *
     * @param value - объект, привязанный к вершине
     */
    public BeanVertex addVertex(Bean value) {
        BeanVertex valueBeanVertex = new BeanVertex(value);
        List<BeanVertex> valueBeanVertexList = new ArrayList<>();

        ArrayList<String> refList = haveProrertyRef(value);
        if (refList != null) {
            valueBeanVertexList = putRefInList(refList);
            vertices.put(valueBeanVertex, valueBeanVertexList);
        } else {
            vertices.put(valueBeanVertex, valueBeanVertexList);
        }
        verticesRefFromValue(valueBeanVertex); //ищем есть ли Классы зависимые от текущего Класса
        return valueBeanVertex;
    }

    private ArrayList<BeanVertex> putRefInList(ArrayList<String> refList) {
        ArrayList<BeanVertex> result = new ArrayList<>();
        for (String ref :
                refList) {
            for (Entry<BeanVertex, List<BeanVertex>> entry :
                    vertices.entrySet()) {
                if (entry.getKey().getBean().getName().equals(ref)) {
                    result.add(entry.getKey());
                }
            }
        }
        return result;
    }

    private void verticesRefFromValue(BeanVertex valueBeanVertex) {
        for (Entry<BeanVertex, List<BeanVertex>> entry :
                vertices.entrySet()) {
            for (Entry<String, Property> entry1 :
                    entry.getKey().getBean().getProperties().entrySet()) {
                if (entry1.getValue().getValue().equals(valueBeanVertex.getBean().getName())) {
                    entry.getValue().add(valueBeanVertex);
                }
            }
        }
    }

    private ArrayList<String> haveProrertyRef(Bean value) {
        ArrayList<String> result = new ArrayList<>();
        for (Entry<String, Property> entry :
                value.getProperties().entrySet()) {
            if (entry.getValue().getType() == ValueType.REF) {
                result.add(entry.getValue().getValue());
            }
        }
        if (result.size() == 0) {
            return null;
        } else {
            return result;
        }
    }

    /**
     * Соединить вершины ребром
     *
     * @param from из какой вершины
     * @param to   в какую вершину
     */
    public void addEdge(BeanVertex from, BeanVertex to) {
        if (!isConnected(from, to)) {
            getLinked(from).add(to);
        }
    }

    /**
     * Проверяем, связаны ли вершины
     */
    public boolean isConnected(BeanVertex v1, BeanVertex v2) {
        return getLinked(v1).contains(v2);
    }

    /**
     * Получить список вершин, с которыми связана vertex
     */
    public List<BeanVertex> getLinked(BeanVertex vertex) {
        return vertices.get(vertex);
    }

    /**
     * Количество вершин в графе
     */
    public int size() {
        return vertices.size();
    }

    @Override
    public String toString() {
        String result = "";
        int i0 = 1;
        for (Entry<BeanVertex, List<BeanVertex>> entry :
                vertices.entrySet()) {
            result += i0 + ". " + entry.getKey() + "\n-->\n" + entry.getValue() + "\n\n";
            i0++;
        }
        return "BeanGraph{\n" +
                "vertices=\n" + result +
                '}';
    }

    public ArrayList<Bean> getBlackListBean(String pathToConfig) throws InvalidConfigurationException {
        BeanGraph beanGraph = new BeanGraph();
        List<Bean> beens = new BeanXmlReader().parseBeans(pathToConfig);
        beens.forEach(beanGraph::addVertex);
        CheckMap checkMap = new CheckMap(beanGraph.vertices);
        checkMap.check();
        ArrayList<Bean> result = new ArrayList<>();
        for (BeanVertex beanV :
                checkMap.getBlackList()) {
            result.add(beanV.getBean());
        }
        return result;
    }


    public static void main(String[] args) throws InvalidConfigurationException {
        BeanGraph beanGraph = new BeanGraph();


        List<Bean> beens = new BeanXmlReader().parseBeans("C:\\temp\\java\\mailru\\messenger\\config1.xml");
        beens.forEach(beanGraph::addVertex);

        System.out.println(beanGraph.toString());

        CheckMap checkMap = new CheckMap(beanGraph.vertices);
        checkMap.check();
        System.out.println(checkMap);


    }


    private static class CheckMap {
        Map<BeanVertex, List<BeanVertex>> vertices = new HashMap<>();
        ArrayList<BeanVertex> whiteList = new ArrayList<>();
        ArrayList<BeanVertex> greyList = new ArrayList<>();
        ArrayList<BeanVertex> blackList = new ArrayList<>();

        public ArrayList<BeanVertex> getBlackList() {
            return blackList;
        }

        @Override
        public String toString() {
            String result = "\nThe order of initialization classes\n";
            for (BeanVertex beanVertex :
                    blackList) {
                result += beanVertex + "\n";
            }
            return result;
        }

        public CheckMap(Map<BeanVertex, List<BeanVertex>> vertices) {
            for (Entry<BeanVertex, List<BeanVertex>> entry : vertices.entrySet()) {
                whiteList.add(entry.getKey());
            }
            this.vertices = new HashMap<>(vertices);
        }

        public void check() throws InvalidConfigurationException {
            for (Entry<BeanVertex, List<BeanVertex>> entry : vertices.entrySet()) {
                BeanVertex beanVertex = entry.getKey();
                if (circleInMap(beanVertex)) {
                    throw new InvalidConfigurationException("The circle init classes found");
                }
            }
        }

        public boolean circleInMap(BeanVertex beanVertex) {
            if (whiteList.contains(beanVertex)) {
                if (greyList.contains(beanVertex)) {
                    return true;
                }
                if (vertices.get(beanVertex).size() != 0) {
                    greyList.add(beanVertex);
                    for (BeanVertex refBean :
                            vertices.get(beanVertex)) {
                        if (!blackList.contains(refBean)) {
                            if (circleInMap(refBean)) {
                                return true;
                            }
                        }
                    }
                    greyList.remove(beanVertex);
                    blackList.add(beanVertex);
                    whiteList.remove(beanVertex);
                } else {
                    blackList.add(beanVertex);
                    whiteList.remove(beanVertex);
                }
            }
            return false;
        }
    }
}
