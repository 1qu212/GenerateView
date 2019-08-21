package cn.xydzjnq.generateview;

import cn.xydzjnq.generateview.util.ClassTypeUtils;
import com.intellij.psi.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

public class TemplateDialog extends JDialog {

    private String viewHolderName;
    private JTextField holderTextField;
    private ArrayList<JTextField> jTextFieldArrayList = new ArrayList<>();
    private ArrayList<JCheckBox> jCheckBoxArrayList = new ArrayList<>();
    private JCheckBox onClickCheckBox;

    public TemplateDialog(PsiElement psiElement, PsiFile psiFile, PsiClass psiClass, ArrayList<Element> elementList) {
        boolean hasViewHolder = false;
        PsiReferenceList psiReferenceList = psiClass.getExtendsList();
        if (psiReferenceList != null) {
            for (PsiJavaCodeReferenceElement element : psiReferenceList.getReferenceElements()) {
                if (ClassTypeUtils.recycleViewAdapters.contains(element.getQualifiedName())) {
                    hasViewHolder = true;
                } else if (ClassTypeUtils.adapters.contains(element.getQualifiedName())) {
                    hasViewHolder = true;
                }
            }
        }
        setSize(new Dimension(640, 480));
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Dimension screenSize = toolkit.getScreenSize();
        int width = screenSize.width;
        int height = screenSize.height;
        int x = (width - 640) / 2;
        int y = (height - 360) / 2;
        setLocation(x, y);
        setTitle("Generate View");
        JPanel container = new JPanel();
        Box box = Box.createVerticalBox();
        JPanel jPanel = new JPanel();
        jPanel.setPreferredSize(new Dimension(600, 35 * elementList.size()));
        jPanel.setLayout(new GridBagLayout());
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        if (hasViewHolder) {
            for (int i = 0; i < elementList.size(); i++) {
                JLabel jLabel = new JLabel(elementList.get(i).getShortType());
                JTextField jTextField = new JTextField(elementList.get(i).getName());
                jTextFieldArrayList.add(jTextField);
                add(jPanel, jLabel, gridBagConstraints, 0, i, 1, 1);
                add(jPanel, jTextField, gridBagConstraints, 1, i, 3, 1);
            }
        } else {
            JPanel clickJPanel = new JPanel();
            JLabel clickLabel = new JLabel("click");
            clickJPanel.add(clickLabel, BorderLayout.WEST);
            clickJPanel.add(Box.createHorizontalStrut(580));
            add(clickJPanel, BorderLayout.NORTH);
            for (int i = 0; i < elementList.size(); i++) {
                JCheckBox jCheckBox = new JCheckBox(elementList.get(i).getShortType());
                jCheckBoxArrayList.add(jCheckBox);
                JTextField jTextField = new JTextField(elementList.get(i).getName());
                jTextFieldArrayList.add(jTextField);
                add(jPanel, jCheckBox, gridBagConstraints, 0, i, 1, 1);
                add(jPanel, jTextField, gridBagConstraints, 1, i, 3, 1);
            }
        }
        box.add(jPanel);
        container.add(box, BorderLayout.CENTER);
        JPanel buttonJPanel = new JPanel();
        JButton confirmJButton = new JButton("OK");
        JButton cancelJButton = new JButton("Cancel");
        buttonJPanel.add(confirmJButton);
        buttonJPanel.add(cancelJButton);
        add(new JScrollPane(container));
        Box vBox = Box.createVerticalBox();
        if (hasViewHolder) {
            String psiClassName = psiClass.getName();
            if (psiClassName.indexOf("Adapter") > -1) {
                psiClassName = psiClassName.substring(0, psiClassName.indexOf("Adapter"));
            }
            viewHolderName = psiClassName + "ViewHolder";
            JLabel jLabel = new JLabel("ViewHolder's name");
            holderTextField = new JTextField(viewHolderName);
            Box viewHolderBox = Box.createHorizontalBox();
            viewHolderBox.add(Box.createHorizontalStrut(10));
            viewHolderBox.add(jLabel);
            viewHolderBox.add(Box.createHorizontalStrut(50));
            viewHolderBox.add(holderTextField);
            viewHolderBox.add(Box.createHorizontalStrut(10));
            vBox.add(viewHolderBox);
            vBox.add(buttonJPanel);
        } else {
            Box onClickJPanel = Box.createHorizontalBox();
            onClickCheckBox = new JCheckBox("onClick");
            onClickJPanel.add(onClickCheckBox, BorderLayout.WEST);
            onClickJPanel.add(Box.createHorizontalStrut(540));
            vBox.add(onClickJPanel);
            vBox.add(buttonJPanel);
        }
        add(vBox, BorderLayout.SOUTH);
        setResizable(false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setVisible(true);
        confirmJButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                String holderName = null;
                if (viewHolderName != null) {
                    holderName = holderTextField.getText().toString().trim();
                }
                TemplateDialog.this.dispose();
                boolean onClick = onClickCheckBox == null ? false : onClickCheckBox.isSelected();
                ArrayList<Element> elementArrayList = new ArrayList<>();
                ArrayList<Element> clickElementArrayList = new ArrayList<>();
                if (jCheckBoxArrayList.size() == 0) {
                    for (int i = 0; i < elementList.size(); i++) {
                        Element modifiedElement = new Element(elementList.get(i));
                        modifiedElement.setName(jTextFieldArrayList.get(i).getText());
                        elementArrayList.add(modifiedElement);
                    }
                } else {
                    for (int i = 0; i < elementList.size(); i++) {
                        Element modifiedElement = new Element(elementList.get(i));
                        modifiedElement.setName(jTextFieldArrayList.get(i).getText());
                        elementArrayList.add(modifiedElement);
                        if (jCheckBoxArrayList.get(i).isSelected()) {
                            clickElementArrayList.add(modifiedElement);
                        }
                    }
                }
                new InjectWriter(psiElement, psiFile, psiClass, elementArrayList, holderName, onClick, clickElementArrayList).execute();
            }
        });
        cancelJButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                TemplateDialog.this.dispose();
            }
        });
    }

    public void add(JPanel jPanel, Component component, GridBagConstraints gridBagConstraints, int gridx, int gridy, int weightx, int weighty) {
        gridBagConstraints.gridx = gridx;
        gridBagConstraints.gridy = gridy;
        gridBagConstraints.weightx = weightx;
        gridBagConstraints.weighty = weighty;
        jPanel.add(component, gridBagConstraints);
    }
}
