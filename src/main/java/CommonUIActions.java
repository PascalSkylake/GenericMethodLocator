import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;

public class CommonUIActions {

    private CommonUIActions() {
    }

    public static abstract class SelectFile extends AbstractAction {

        private String defaultInputFileLocation;
        private final boolean isSave;

        public SelectFile() {
            this("Select");
        }

        public SelectFile(String name) {
            this(name,System.getProperty("user.dir"));
        }

        public SelectFile(String name, String inputFileLocation) {
            this(name,inputFileLocation, false);
        }

        public SelectFile(String name, String inputFileLocation, boolean isSave) {
            super(name);
            this.defaultInputFileLocation = inputFileLocation;
            this.isSave = isSave;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JFileChooser fileChooser = new JFileChooser(new File(defaultInputFileLocation));
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            int choice = (isSave) ? fileChooser.showSaveDialog(null) : fileChooser.showOpenDialog(null);
            if (choice != JFileChooser.APPROVE_OPTION) {
                return;
            }

            File selectedFile = fileChooser.getSelectedFile();
            doWithSelectedDirectory(selectedFile);
            if(selectedFile!=null)
                defaultInputFileLocation = selectedFile.getAbsolutePath();
        }

        public abstract void doWithSelectedDirectory(File selectedFile);
    }

}