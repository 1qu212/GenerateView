package cn.xydzjnq.generateview;

import java.util.ArrayList;

public class ElementType {
    public static final int TYPE0 = 0;
    public static final int TYPE1 = 1;
    public static final int TYPE2 = 2;
    private int type = TYPE0;
    ArrayList<Element> elementArrayList = new ArrayList<>();

    private ElementType() {
    }

    private static ElementType elementType;

    public static ElementType getInstance() {
        if (elementType == null) {
            synchronized (ElementType.class) {
                if (elementType == null) {
                    elementType = new ElementType();
                }
            }
        }
        return elementType;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
        for (Element element : elementArrayList) {
            element.initName();
        }
    }

    public void addElement(Element element) {
        elementArrayList.add(element);
    }
}
