package cz.kamma.mimeed.ui;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.StringReader;
import java.util.Base64;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class MimeEDApp {

	private JFrame frmMimeed;
	JTextArea textArea;
	private JButton btnDecodeAndSave;
	private JButton btnClear;
	private JLabel lblBytes;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					MimeEDApp window = new MimeEDApp();
					window.frmMimeed.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public MimeEDApp() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		try {
			UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
		} catch (Exception e) {
		}
		frmMimeed = new JFrame();
		frmMimeed.setTitle("MimeED (Encode with Drag-and-Drop files into window)");
		frmMimeed.setBounds(100, 100, 739, 506);
		frmMimeed.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frmMimeed.getContentPane().setLayout(new BorderLayout(0, 0));

		textArea = new JTextArea();
		textArea.getDocument().addDocumentListener(new DocumentListener() {

	        @Override
	        public void removeUpdate(DocumentEvent e) {
				lblBytes.setText(textArea.getText().length() + " bytes");
	        }

	        @Override
	        public void insertUpdate(DocumentEvent e) {
				lblBytes.setText(textArea.getText().length() + " bytes");
	        }

	        @Override
	        public void changedUpdate(DocumentEvent arg0) {
				lblBytes.setText(textArea.getText().length() + " bytes");
	        }
	    });
		JScrollPane sp = new JScrollPane(textArea);
		textArea.setWrapStyleWord(true);
		textArea.setColumns(80);
		textArea.setLineWrap(true);
		frmMimeed.getContentPane().add(sp, BorderLayout.CENTER);
		textArea.setDropTarget(new DropTarget() {
			private static final long serialVersionUID = 1L;

			public synchronized void drop(DropTargetDropEvent evt) {
				try {
					evt.acceptDrop(DnDConstants.ACTION_COPY);
					List<File> droppedFiles = (List<File>) evt.getTransferable()
							.getTransferData(DataFlavor.javaFileListFlavor);
					for (File file : droppedFiles) {
						encodeFile(file);
					}
				} catch (Exception ex) {
					JOptionPane.showMessageDialog(frmMimeed,
							"Error occured while Drag'n'Drop file.\nError: " + ex.getMessage(), "Cannot open file",
							JOptionPane.ERROR_MESSAGE);
				}
			}
		});

		JPanel panel = new JPanel();
		frmMimeed.getContentPane().add(panel, BorderLayout.SOUTH);

		btnDecodeAndSave = new JButton("Decode and save");
		btnDecodeAndSave.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JFileChooser fc = new JFileChooser();
				fc.setCurrentDirectory(new File("."));
				fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				int returnVal = fc.showSaveDialog(frmMimeed);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					File file = fc.getSelectedFile();
					try {
						decodeAndSave(file);
					} catch (Exception ex) {
						JOptionPane.showMessageDialog(frmMimeed,
								"Error occured while saving the file.\nError: " + ex.getMessage(), "Cannot save file",
								JOptionPane.ERROR_MESSAGE);
					}
				}
			}
		});

		lblBytes = new JLabel("0 bytes");
		panel.add(lblBytes);
		panel.add(btnDecodeAndSave);

		btnClear = new JButton("Clear");
		btnClear.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textArea.setText("");
			}
		});
		panel.add(btnClear);
	}

	protected void decodeAndSave(File file) throws Exception {
		BufferedReader br = new BufferedReader(new StringReader(textArea.getText()));
		for (String line = br.readLine(); line != null;) { //read header line 1
			br.readLine();//read header line 2
			br.readLine();//read header line 3
			String header4 = br.readLine();
			String filename = header4.split("filename=")[1].replaceAll("\"", "");
			br.readLine();
			String body = "";
			line = br.readLine();
			while (line != null && !line.equals("")) {
				body = body.concat(line);
				line = br.readLine();
			}
			line = br.readLine();
			FileOutputStream fos = new FileOutputStream(new File(file, filename));
			fos.write(Base64.getDecoder().decode(body));
			fos.flush();
			fos.close();
		}
	}

	protected void encodeFile(File file) throws Exception {
		FileInputStream fis = new FileInputStream(file);
		byte[] bin = new byte[fis.available()];
		fis.read(bin);
		fis.close();
		String res = Base64.getEncoder().encodeToString(bin);
		StringReader reader = new StringReader(res);
		StringBuilder all = new StringBuilder();
		int i=0;
		while (reader.ready()) {
			char[] tmp = new char[80];
			if (reader.read(tmp)==-1)
				break;
			i = i + 80;
			all.append(tmp);
			all.append('\n');
		}
		String header = "MIME-Version: 1.0\nContent-Type: application/octet-stream; name=\"" + file.getName()
				+ "\"\nContent-Transfer-Encoding: base64\nContent-Disposition: attachment; filename=\"" + file.getName()
				+ "\"\n\n";
		textArea.append(header);
		textArea.append(all + "\n");
	}

}
