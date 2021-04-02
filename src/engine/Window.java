package engine;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;

import javax.swing.JFrame;
import javax.swing.UIManager;

public class Window {

	private JFrame frame;
	private Component root;
	
	private int frameHeaderWidth, frameHeaderHeight;
	
	public Window() {
		setUILookAndFeel(UIManager.getSystemLookAndFeelClassName());
	}
	
	public void init(String title, int width, int height, boolean resizable, WindowAdapter onCloseListener) {
		
		frame = new JFrame(title);
		
		Dimension dimension = new Dimension(width, height);
		Component root = frame.getContentPane();
		root.setMinimumSize(dimension);
		root.setMaximumSize(dimension);
		root.setPreferredSize(dimension);

		frame.pack();
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		frame.addWindowListener(onCloseListener);
		
		frame.setResizable(resizable);
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
		
		frameHeaderHeight = frame.getHeight() - dimension.height;
		frameHeaderWidth = frame.getWidth() - dimension.width;
	}

	public void setTitle(String title) {
		frame.setTitle(title);
	}
	
	public JFrame getFrame() {
		return frame;
	}
	
	public void setRoot(Component newRoot) {
		if (root != null) frame.remove(root);
		frame.add(newRoot);
		frame.revalidate();
		frame.repaint();
		newRoot.setFocusable(true);
		// newRoot.requestFocusInWindow();
		root = newRoot;
	}

	public int getWidth() {
		return frame.getWidth() - frameHeaderWidth;
	}

	public int getHeight() {
		return frame.getHeight() - frameHeaderHeight;
	}

	private void setUILookAndFeel(String uiName) {
		try {
			UIManager.setLookAndFeel(uiName);
			JFrame.setDefaultLookAndFeelDecorated(true);
		} catch (Exception e) {
			System.out.printf("invalid ui theme: %s\n", uiName);
		}
	}

	public void close() {
		frame.setVisible(false);
		frame.dispose();
		System.exit(0);
	}
}
