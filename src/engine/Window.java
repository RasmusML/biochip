package engine;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.lang.reflect.InvocationTargetException;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Window {

	private JFrame frame;
	private Component root;

	private Dimension dimensions;

	public Window() {
		//setUILookAndFeel(UIManager.getSystemLookAndFeelClassName());
	}

	public void init(String title, int width, int height, boolean resizable, WindowAdapter onCloseListener) {
		frame = new JFrame(title);

		dimensions = new Dimension();

		Dimension initialDimensions = new Dimension(width, height);
		Component root = frame.getContentPane();
		root.setMinimumSize(initialDimensions);
		root.setMaximumSize(initialDimensions);
		root.setPreferredSize(initialDimensions);

		ComponentAdapter listener = new ComponentAdapter() {

			@Override
			public void componentResized(ComponentEvent e) {
				dimensions.setSize(frame.getWidth(), frame.getHeight());
			}
		};

		frame.addComponentListener(listener);

		frame.pack();
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		frame.addWindowListener(onCloseListener);
		frame.setResizable(resizable);
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}

	public void setTitle(String title) {
		frame.setTitle(title);
	}

	public JFrame getFrame() {
		return frame;
	}

	public void setRoot(Component newRoot) {
		if (SwingUtilities.isEventDispatchThread()) {
			setRootUnsafe(newRoot);
		} else {
			try {
				SwingUtilities.invokeAndWait(() -> {
					setRootUnsafe(newRoot);
				});
			} catch (InvocationTargetException | InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public void setRootUnsafe(Component newRoot) {
		if (root != null) frame.remove(root);
		frame.add(newRoot);
		frame.revalidate();
		frame.repaint();
		newRoot.setFocusable(true);
		// newRoot.requestFocusInWindow();
		root = newRoot;
	}

	public int getWidth() {
		return dimensions.width;
	}

	public int getHeight() {
		return dimensions.height;
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
	}
}
