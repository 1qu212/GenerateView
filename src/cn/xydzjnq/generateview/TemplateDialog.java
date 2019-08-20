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

    public TemplateDialog(PsiElement psiElement, PsiFile psiFile, PsiClass psiClass, ArrayList<Element> elementList) {
        setSize(new Dimension(640, 360));
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
        ArrayList<JTextField> jTextFieldArrayList = new ArrayList<>();
        for (int i = 0; i < elementList.size(); i++) {
            JLabel jLabel = new JLabel(elementList.get(i).getShortType());
            JTextField jTextField = new JTextField(elementList.get(i).getName());
            jTextFieldArrayList.add(jTextField);
            add(jPanel, jLabel, gridBagConstraints, 0, i, 1, 1);
            add(jPanel, jTextField, gridBagConstraints, 1, i, 3, 1);
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
        if (hasViewHolder) {
            String psiClassName = psiClass.getName();
            if (psiClassName.indexOf("Adapter") > -1) {
                psiClassName = psiClassName.substring(0, psiClassName.indexOf("Adapter"));
            }
            viewHolderName = psiClassName + "ViewHolder";
            JLabel jLabel = new JLabel("ViewHolder名");
            holderTextField = new JTextField(viewHolderName);
            Box viewHolderPanel = Box.createHorizontalBox();
            viewHolderPanel.add(Box.createHorizontalStrut(10));
            viewHolderPanel.add(jLabel);
            viewHolderPanel.add(Box.createHorizontalStrut(20));
            viewHolderPanel.add(holderTextField);
            viewHolderPanel.add(Box.createHorizontalStrut(10));
            vBox.add(viewHolderPanel);
            vBox.add(buttonJPanel);
        } else {
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
                ArrayList<Element> elementArrayList = new ArrayList<>();
                for (int i = 0; i < elementList.size(); i++) {
                    Element modifiedElement = new Element(elementList.get(i));
                    modifiedElement.setName(jTextFieldArrayList.get(i).getText());
                    elementArrayList.add(modifiedElement);
                }
                new InjectWriter(psiElement, psiFile, psiClass, elementArrayList, holderName).execute();
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
