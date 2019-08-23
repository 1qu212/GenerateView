package cn.xydzjnq.generateview;

import javax.swing.*;

public class Element {
    private String type;
    private String shortType;
    private String id;
    private String name;

    private JTextField jTextField;

    public Element(Element element) {
        this.type = element.type;
        this.shortType = element.shortType;
        this.id = element.id;
        this.name = element.name;
    }

    public Element(String type, String id) {
        this.type = type;
        this.shortType = initShortType();
        this.id = id;
        this.name = initName_0();
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getShortType() {
        return shortType;
    }

    public void setShortType(String shortType) {
        this.shortType = shortType;
    }

    private String initShortType() {
        //这里不需要转义
        if (type.contains(".")) {
            //这里需要转义
            String[] strings = type.split("\\.");
            return strings[strings.length - 1];
        }
        return type;
    }

    public void initName() {
        switch (ElementType.getInstance().getType()) {
            default:
                name = initName_0();
                break;
            case ElementType.TYPE1:
                name = initName_1();
                break;
            case ElementType.TYPE2:
                name = initName_2();
                break;
        }
        jTextField.setText(name);
    }

    public void setjTextField(JTextField jTextField) {
        this.jTextField = jTextField;
    }

    private String initName_0() {
        String[] words = id.split("_");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            char[] chars = words[i].toCharArray();
            if (i > 0) {
                chars[0] = Character.toUpperCase(chars[0]);
            }
            sb.append(chars);
        }
        return sb.toString();
    }

    private String initName_1() {
        String[] words = id.split("_");
        StringBuilder sb = new StringBuilder();
        sb.append("m");
        for (int i = 0; i < words.length; i++) {
            char[] chars = words[i].toCharArray();
            chars[0] = Character.toUpperCase(chars[0]);
            sb.append(chars);
        }
        return sb.toString();
    }

    private String initName_2() {
        return id;
    }
}
