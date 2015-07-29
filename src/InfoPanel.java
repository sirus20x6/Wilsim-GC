import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class InfoPanel
{
	JTextArea log;

	public void init()
	{
		log=new JTextArea(8,50);
		JScrollPane bar = new JScrollPane(log);
	}
}
