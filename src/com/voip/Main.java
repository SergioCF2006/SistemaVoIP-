package com.voip;

import com.voip.gui.VoipMainFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Punto de entrada principal para ejecutar la aplicación Sistema VoIP.
 */
public class Main {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            VoipMainFrame frame = new VoipMainFrame();
            frame.setVisible(true);
        });
    }
}
