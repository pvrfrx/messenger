package arhangel.dim.container;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;


/**
 * Используйте ваш xml reader чтобы прочитать конфиг и получить список бинов
 */
public class Container {
    private List<Bean> beans;
    Map<String, Object> objByName = new HashMap<>();
    Map<String, Object> objByClassName = new HashMap<>();


    /**
     * Если не получается считать конфиг, то бросьте исключение
     *
     * @throws InvalidConfigurationException неверный конфиг
     */
    public Container(String pathToConfig) throws InvalidConfigurationException {
        // вызываем BeanXmlReader
        BeanGraph beanGraph = new BeanGraph();
        beans = beanGraph.getBlackListBean(pathToConfig);
    }

    /**
     * Вернуть объект по имени бина из конфига
     * Например, Car car = (Car) container.getByName("carBean")
     */
    public Object getByName(String name) {

        for (Entry<String, Object> entry :
                objByName.entrySet()) {
            if (entry.getKey().equals(name)) {
                return entry.getValue();
            }
        }

        return new InvalidConfigurationException("This class didn't found in container");
    }

    /**
     * Вернуть объект по имени класса
     * Например, Car car = (Car) container.getByClass("arhangel.dim.container.Car")
     */
    public Object getByClass(String className) {
        for (Entry<String, Object> entry :
                objByClassName.entrySet()) {
            if (entry.getKey().equals(className)) {
                return entry.getValue();
            }
        }

        return new InvalidConfigurationException("This class didn't found in container");
    }

    private Object instantiateBean(Bean bean) throws InvalidConfigurationException {
        // Примерный ход работы
        String className = bean.getClassName();
        Class clazz = null;
        try {
            clazz = Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new InvalidConfigurationException("This class didn't found: " + className);
        }

        // ищем дефолтный конструктор
        Object ob = null;
        try {
            ob = clazz.newInstance();
        } catch (Exception e) {
            throw new InvalidConfigurationException("For this class didn't create default constructor for this class: " + className);
        }

        //заполняем все properties
        for (String name : bean.getProperties().keySet()) {
            // ищем поле с таким именен внутри класса, учитывая приватные
            // проверяем, если такого поля нет, то кидаем InvalidConfigurationException с описанием ошибки
            Field field = null;
            try {
                field = clazz.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                throw new InvalidConfigurationException("Field in class didn't found: " + name);
            }

            // Делаем приватные поля доступными
            field.setAccessible(true);

            // Далее определяем тип поля и заполняем его
            if (field.getType().isPrimitive()) { // Если поле - примитив, то все просто
                if (field.getType().getName().equals("int")) {
                    String nameMethod = "set" + firstCharUpperCase(field.getName());
                    Method method = null;
                    try {
                        method = clazz.getMethod(nameMethod, int.class);
                    } catch (NoSuchMethodException e) {
                        throw new InvalidConfigurationException("Setter for this field didn't found " + field.getName());
                    }

                    try {
                        method.invoke(ob, Integer.parseInt(bean.getProperties().get(name).getValue()));
                    } catch (Exception e) {
                        throw new InvalidConfigurationException("Cann't use this method: " + method.getName());
                    }
                } else {
                    throw new InvalidConfigurationException("I don't know how work in other primitive types, only int");
                }
            } else { // Если поле ссылка, то эта ссылка должа была быть инициализирована ранее
                String nameMethod = "set" + firstCharUpperCase(field.getName());
                Method method = null;
                try {
                    method = clazz.getMethod(nameMethod,
                            getByName(bean.getProperties().get(name).getValue()).getClass());
                } catch (NoSuchMethodException e) {
                    throw new InvalidConfigurationException("Setter for this field didn't found " + field.getName());
                }
                try {
                    method.invoke(ob, getByName(bean.getProperties().get(name).getValue()));
                } catch (Exception e) {
                    throw new InvalidConfigurationException("Cann't use this method: " + method.getName());
                }
            }
        }
        return ob;
    }

    private String firstCharUpperCase(String name) {
        String result = "";
        char[] chars = name.toCharArray();
        result += Character.toUpperCase(chars[0]);
        for (int i = 1; i < chars.length; i++) {
            result += chars[i];
        }
        return result;
    }

    public static void main(String[] args) throws InvalidConfigurationException {
        Container container = new Container("C:\\temp\\java\\mailru\\messenger\\config.xml");
        for (Bean bean :
                container.beans) {
            Object object = container.instantiateBean(bean);
            container.objByName.put(bean.getName(), object);
            container.objByClassName.put(bean.getClassName(), object);
        }
        System.out.println(container.objByName);
    }
}
