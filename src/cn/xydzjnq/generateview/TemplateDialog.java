package cn.xydzjnq.generateview;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

public class TemplateDialog extends JDialog {
    public TemplateDialog(ArrayList<Element> elementList) {
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
        for (int i = 0; i < elementList.size(); i++) {
            JLabel jLabel = new JLabel(elementList.get(i).getType());
            JTextField jTextField = new JTextField(elementList.get(i).getName());
            add(jPanel, jLabel, gridBagConstraints, 0, i, 1, 1);
            add(jPanel, jTextField, gridBagConstraints, 1, i, 3, 1);
        }
        box.add(jPanel);
        container.add(box, BorderLayout.CENTER);
        add(new JScrollPane(container));
        setResizable(false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setVisible(true);
    }

    public void add(JPanel jPanel, Component component, GridBagConstraints gridBagConstraints, int gridx, int gridy, int weightx, int weighty) {
        gridBagConstraints.gridx = gridx;
        gridBagConstraints.gridy = gridy;
        gridBagConstraints.weightx = weightx;
        gridBagConstraints.weighty = weighty;
        jPanel.add(component, gridBagConstraints);
    }
}
