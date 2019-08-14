package cn.xydzjnq.generateview;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

public class TemplateDialog extends JDialog {
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
        add(buttonJPanel, BorderLayout.SOUTH);
        setResizable(false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setVisible(true);
        confirmJButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                ArrayList<Element> elementArrayList = new ArrayList<>();
                for (int i = 0; i < elementList.size(); i++) {
                    Element modifiedElement = new Element(elementList.get(i));
                    modifiedElement.setName(jTextFieldArrayList.get(i).getText());
                    elementArrayList.add(modifiedElement);
                }
                new InjectWriter(psiElement, psiFile, psiClass, elementArrayList).execute();
                TemplateDialog.this.dispose();
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
