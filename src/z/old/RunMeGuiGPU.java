package z.old;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.UIManager;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLJPanel;
import com.jogamp.opengl.glu.GLU;

public class RunMeGuiGPU {

	public static void main(String[] args) throws Exception {
		//GLProfile glprofile = GLProfile.getDefault();
		//GLProfile.initSingleton();
		//GLCapabilities glcapabilities = new GLCapabilities();
		GLJPanel gljpanel = new GLJPanel();
		gljpanel.setPreferredSize(new Dimension(440, 480));

		gljpanel.addGLEventListener(new GLEventListener() {

			@Override
			public void reshape(GLAutoDrawable glautodrawable, int x, int y, int width, int height) {
				OneTriangle.setup(glautodrawable.getGL().getGL2(), width, height);
			}

			@Override
			public void init(GLAutoDrawable glautodrawable) {
			}

			@Override
			public void dispose(GLAutoDrawable glautodrawable) {
			}

			@Override
			public void display(GLAutoDrawable glautodrawable) {
				OneTriangle.render(glautodrawable.getGL().getGL2(), glautodrawable.getSurfaceWidth(),
						glautodrawable.getSurfaceHeight());
			}
		});

		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

		JPanel panel = new JPanel();
		panel.setPreferredSize(new Dimension(200, 480));

		for (int i = 0; i < 10; i++) {
			String text = String.format("button %d", i);
			JButton button = new JButton(text);
			panel.add(button);
		}

		class BoolWrapper {
			boolean bool;
		}

		BoolWrapper runningWrapper = new BoolWrapper();
		runningWrapper.bool = true;

		JFrame jframe = new JFrame("One Triangle Swing GLJPanel");
		jframe.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent e) {
				super.windowClosed(e);

				runningWrapper.bool = false;
			}
		});

		jframe.getContentPane().add(gljpanel, BorderLayout.CENTER);
		jframe.getContentPane().add(panel, BorderLayout.WEST);

		jframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		jframe.setSize(640, 480);
		jframe.setVisible(true);

		while (runningWrapper.bool) {
			gljpanel.display();

			Thread.sleep(16);
		}
	}
}
