
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import com.swirlds.platform.Browser;
import com.swirlds.platform.Platform;
import com.swirlds.platform.SwirldMain;
import com.swirlds.platform.SwirldState;

//Possible operation types
//noOp							- type 0
//isn(position,element);		- type 1
//del(position);				- type 2

public class HelloSwirldDemoMain implements SwirldMain {
	
	//pending operations list + log of the all user-operations
	private List<Operation> pending_operations	= new ArrayList<Operation>();
		
	private int operation_counter 	= 0;	//iterator on received operations
	private int operation_iter		= 0;	//iterator on pending list operations
	private Operation tmpOperation 	= null;	//tmp Operation object
		
	public Platform		platform;			// the application is run by this
	public int			selfId;				// ID number for this member
	public final int	sleepPeriod	= 10;	// sleep this many milliseconds after each sync
	
	//Graphic interface elements
	int w_width = 500;
	int w_height = 400;
	
	private JFrame jframe;
	private JPanel jpanel_top;
	private JPanel jpanel_bot;
	
	private JMenuBar menuBar;
	
	private JMenu file_jmenu;
	private JMenu help_jmenu;
	
	private JMenuItem menuitem_open;
	private JMenuItem menuitem_exit;
	private JMenuItem menuitem_save;
	private JMenuItem menuitem_about;
	
	private JTextArea jtextarea;
	
	private JLabel statusbar;
	
	//for the insert implementation
	private int last_keycode = 0;
	private int last_caretposition = 0;

	/**
	 * This is just for debugging: it allows the app to run in Eclipse. If the config.txt exists and lists a
	 * particular SwirldMain class as the one to run, then it can run in Eclipse (with the green triangle
	 * icon).
	 * 
	 * @param args
	 * these are not used
	 */
	public static void main(String[] args) {
		Browser.main(null);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void preEvent() {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void init(Platform platform, int id) {
		
		this.platform = platform;
		this.selfId = id;
		platform.setAbout("Hello Swirld v. 1.0\n"); // set the browser's "about" box
		platform.setSleepAfterSync(sleepPeriod);
		

		/*********************** User-Interface settings *******************************/
		//jframe creation and initialization
		this.jframe = new JFrame("Test " + this.selfId);
		this.jframe.setLayout(new BorderLayout());
		
		this.menuBar = new JMenuBar();
		this.file_jmenu = new JMenu("File");
		this.help_jmenu = new JMenu("Help");
		
		this.menuitem_open = new JMenuItem("Open");
		this.menuitem_save = new JMenuItem("Save");
		this.menuitem_exit = new JMenuItem("Exit");
		this.menuitem_about = new JMenuItem("About");
		
		file_jmenu.add(menuitem_open);
		file_jmenu.add(menuitem_save);
		file_jmenu.add(menuitem_exit);
		help_jmenu.add(menuitem_about);
		
		menuBar.add(file_jmenu);
		menuBar.add(help_jmenu);
		
		
		//we will have 2 pannels (top - with text edition area + bot - with buttons)
		this.jpanel_top = new JPanel(new GridLayout(1,1));
		this.jpanel_top.setPreferredSize(new Dimension(200,320));
		
		this.jpanel_bot = new JPanel(new GridLayout(1,2));
		this.jpanel_bot.setPreferredSize(new Dimension(50,50));
		
		this.jtextarea = new JTextArea();
		this.jtextarea.setText("");
		this.jtextarea.setLineWrap(true);
				
		this.statusbar = new JLabel();
		
		//elements adding
		this.jpanel_top.add(jtextarea);
		this.jpanel_bot.add(statusbar);
		
		this.jframe.add(jpanel_top,BorderLayout.NORTH);
		this.jframe.add(jpanel_bot);
		
		//buttons listeners setting
		setListeners();
		
		//jframe settings
		this.jframe.setJMenuBar(menuBar);
		this.jframe.setMinimumSize(new Dimension(w_width, w_height));
		
		double width = Toolkit.getDefaultToolkit().getScreenSize().getWidth();
		double height = Toolkit.getDefaultToolkit().getScreenSize().getHeight();
		
		this.jframe.setResizable(false);
		this.jframe.setBounds((int)width/2-w_width/2, (int)height/2-w_height/2, w_width, w_height);
		this.jframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.jframe.pack();
		this.jframe.setVisible(true);
	}
	
	
	//setting all the button-listeners
	private void setListeners(){
		
		//file opening
		this.menuitem_open.addActionListener(new ActionListener(){
			
			public void actionPerformed(ActionEvent e)
			{
				JFileChooser chooser = new JFileChooser();
				chooser.setCurrentDirectory(new File( "./"));
				
				if (chooser.showDialog(chooser, "Open") == JFileChooser.APPROVE_OPTION)
				{
					try {
						BufferedReader reader = new BufferedReader(new FileReader(chooser.getSelectedFile()));
						
					    StringBuilder sb = new StringBuilder();
					    String line = reader.readLine();
					    
					    while (line != null) {
					        sb.append(line);
					        sb.append(System.lineSeparator());
					        line = reader.readLine();
					    }
					    
					    String everything = sb.toString();
					    
					    //When we decided to open the document, that means that
					    //we don't need anything at the view, and we need to clean it.
					    
					    if(jtextarea.getText().length()!=0)
						{	
							for(int i = 0; i<jtextarea.getText().length(); i++)
							{
								String command_buf = "del(" + 1 + ")";
								tmpOperation = new Operation(command_buf,selfId,Instant.now().toEpochMilli(), operation_counter);
								pending_operations.add(tmpOperation);
							};
						}
		
					    jtextarea.setText(everything);
					    
						for(int i = 0; i<everything.length(); i++)
						{
							String command_buf = "ins(" +  (last_caretposition+i) + "," + everything.charAt(i) + ")";
							tmpOperation = new Operation(command_buf,selfId,Instant.now().toEpochMilli(), operation_counter);
							pending_operations.add(tmpOperation);
						};
					    
					    reader.close();
					    System.out.println("File: " + chooser.getSelectedFile().getAbsolutePath() +  " opened.");
					} catch(IOException e1)
					{
						System.out.println("File reading problem.");
					}
				};
			};
		});
		
		
		//about window
		this.menuitem_about.addActionListener(new ActionListener(){
			
			public void actionPerformed(ActionEvent e)
			{
				 final String text = "Here you can find some information about the app and developers. If you have any questions, "
				 		+ "you can email us: info@info.com.";
				 final String html = "<html><body style='width: 250px'>";
					 
				 JOptionPane.showMessageDialog(
		                       null, new JLabel(html + text), "About", JOptionPane.INFORMATION_MESSAGE);
			};
		});
			
		//exit button
		this.menuitem_exit.addActionListener(new ActionListener()
		{
			  public void actionPerformed(ActionEvent e)
			  {
			    System.exit(0);
			  };
		});
		
		//save button
		this.menuitem_save.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				try{
					JFileChooser chooser = new JFileChooser();
					chooser.setCurrentDirectory(new File( "./"));
					if (chooser.showSaveDialog(chooser) == JFileChooser.APPROVE_OPTION)
					{
						File fileName = new File(chooser.getSelectedFile() + ".txt");
						BufferedWriter outFile = new BufferedWriter(new FileWriter(fileName));
						outFile.write(jtextarea.getText());
						outFile.flush();
						outFile.close();
					}
				}
				catch(IOException exc){
					System.out.println("File writing problems.");
					exc.printStackTrace();
				};
			};
		});
	};
	

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void run() {
		
		String myName = platform.getState().getAddressBookCopy()
				.getAddress(selfId).getSelfName();

		//getting the IP adress
		String ip = "";
		byte [] ipAddr = platform.getAddress().getAddressExternalIpv4();
				
		for(int i = 0; i<ipAddr.length; i++)
			ip += String.valueOf(ipAddr[i]);
		
		//attaching the address to the label at the bottom
		statusbar.setText("Platform id [" + this.selfId + "] [" + ip + ":" + platform.getAddress().getPortExternalIpv4() + "]");

		// Send the transaction to the Platform, which will then
		// forward it to the State object.
		// The Platform will also send the transaction to
		// all the other members of the community during syncs with them.
		// The community as a whole will decide the order of the transactions
		
		//key listener for the jtextarea
		jtextarea.addKeyListener(new KeyListener(){

			@Override
			public void keyPressed(KeyEvent arg0) {
				// TODO Auto-generated method stub

				last_keycode = arg0.getKeyCode();
				String command_buf = "";
				
				//case of delete operation
				if(arg0.getKeyCode() == 8 || arg0.getKeyCode() == 127 ) //backspace or delete buttons
				{
					//if we selected some block of the text to delete, we need to get data from this block
					if(jtextarea.getSelectedText()!=null)
					{	
						for(int i = jtextarea.getSelectionStart()+1; i<jtextarea.getSelectionEnd()+1; i++)
						{
							command_buf = "del(" +  (jtextarea.getSelectionStart()+1) + ")";
							tmpOperation = new Operation(command_buf,selfId,Instant.now().toEpochMilli(), operation_counter);
							pending_operations.add(tmpOperation);
						};
					}
					else{	//if we just pressed one button to delete one character
							
						//defining delete and backspace
						if(arg0.getKeyCode() == 8)
							command_buf = "del(" +  jtextarea.getCaretPosition() + ")";
						else
							command_buf = "del(" +  (jtextarea.getCaretPosition()+1) + ")";
							
						tmpOperation = new Operation(command_buf,selfId,Instant.now().toEpochMilli(), operation_counter);
						pending_operations.add(tmpOperation);
					}
				};
				
				//insert case
				if(	arg0.getKeyCode() == 32 ||
					arg0.getKeyCode() == 9	|| 
					arg0.getKeyCode() == 10	||
					arg0.getKeyCode() > 40	)
				{
					//special case to manage ctrl+v operation
					if(arg0.getKeyCode()==86 && (arg0.getKeyChar()!='V' || arg0.getKeyChar()!='v'))
					{ 
						//this case is to skip the problem :)
					}
					else
					{
						command_buf = "ins(" +  jtextarea.getCaretPosition() + "," + arg0.getKeyChar() + ")";
						tmpOperation = new Operation(command_buf,selfId,Instant.now().toEpochMilli(), operation_counter);
						pending_operations.add(tmpOperation);
					}
				};
				
				//saving the caret position
				last_caretposition = jtextarea.getCaretPosition();
			};

			@Override
			public void keyReleased(KeyEvent arg0) {
				// TODO Auto-generated method stub
					
				//if we pressed CTRL+V (insert block) and carret moved more that on 1 symbol
				if( (last_keycode == 86 || last_keycode == 118) && (Math.abs(jtextarea.getCaretPosition()-last_caretposition)>1) )
				{
					String insert = jtextarea.getText().substring(last_caretposition, jtextarea.getCaretPosition());
					
					for(int i = 0; i<insert.length(); i++)
					{
						String command_buf = "ins(" +  (last_caretposition+i) + "," + insert.charAt(i) + ")";
						tmpOperation = new Operation(command_buf,selfId,Instant.now().toEpochMilli(), operation_counter);
						pending_operations.add(tmpOperation);
					};
					
					last_keycode = 0;
				};
			};

			@Override
			public void keyTyped(KeyEvent arg0) {
				// TODO Auto-generated method stub
			};
		});
		
		
		while (platform.isRunning()) {
				
			if(this.operation_iter<this.pending_operations.size()){
				
				//getting the operation and making a transaction
				platform.createTransaction(pending_operations.get(operation_iter).toString().getBytes(StandardCharsets.UTF_8), null);	
				
				//cleaning the values
				tmpOperation 	= null;
				this.operation_iter++;
			};
			
			//getting the state and data from the platform
			HelloSwirldDemoState server_state = (HelloSwirldDemoState) platform.getState();
			Operation received_operation = server_state.getOperation(operation_counter);
									
			//now we are receiving 
			if(received_operation != null)
			{		
				operation_counter++;

				//if it's not our own operation 
				if(received_operation.get_operationID()!=this.selfId){

					//transformation
					if(this.operation_iter<this.pending_operations.size())
					{
						for(int i = this.operation_iter; i<this.pending_operations.size(); i++)
							received_operation = HelloSwirldDemoState.transformation(received_operation, this.pending_operations.get(i));
					};

					applyOperation(received_operation);
				}; 
			};
			
			//sleep to get control
			try {
				Thread.sleep(sleepPeriod);
			} catch (Exception e) { }
			
		}; //end of the platform.running while
		
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public SwirldState newState() {
		return new HelloSwirldDemoState();
	}
	
	private void insert(int position, String value)
	{
		if(position >= 0)
		{
			if(jtextarea.getText().length()<position)
			{
				for(int i = jtextarea.getText().length(); i<position; i++)
					jtextarea.setText(jtextarea.getText()+" ");
			}
			
			String tmp = jtextarea.getText().substring(0, position) + 
						value + 
						jtextarea.getText().substring(position, jtextarea.getText().length());

			jtextarea.setText( tmp );
		}
	};
	
	private void delete(int position)
	{		
		if(position>=0 || position<jtextarea.getText().length()){
			
			if(position!=0){
				String tmp = jtextarea.getText().substring(0, position-1) + 
							jtextarea.getText().substring(position,jtextarea.getText().length());
				jtextarea.setText(tmp);
			};
		};
	};
		
	//applying the operation
	private boolean applyOperation(Operation op)
	{
		//just debugging output msg
		System.out.println("["+selfId+"] Apply: " + op);
		
		switch(op.get_operationType())
		{
			//no operation case
			case 0: {
				return true;
			}
		
			//insert
			case 1:{
				insert(op.getPosition(),op.getValue());
				return true;
			}
			
			//delete
			case 2:{
				delete(op.getPosition());
				return true;
			}
			
			default:{
				System.out.println("Operation error."); //this case is impossible
				return false;
			}
		}
	};
	
}