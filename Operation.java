import java.nio.charset.StandardCharsets;

/*
 *	Yevhenii Semenko
 * 
 * - operation view [operation][id][time_stamp]
 * - example [ins(1,1)][0][1234567890];
 * - example [del(1)][0][1234567890];
 * 
 */

public class Operation {
	
	//possible operation types
	private String noop = "noop";	//no operation - no transformation 	- type 0
	private String ins = "ins";		//isn(position,element);			- type 1
	private String del = "del";		//del(position);					- type 2
		
	//type of the operation
	private int operation_Type 	= 0;
	
	//id of the operation (source)
	private int source_ID 	= 0;
	
	//time stamp of the operation
	private long operation_TS = 0;
	
	
	//functions-arguments , if (-1) = not defined.
	private int op_index = 0;	//operation index of the current state
	private int position = -1;
	private String value = "";
	
	//constructors
	
	public Operation() {};
	
	public Operation(String command, int id, long time, int op_index){ 
		
		this.operation_Type = retrieveOperationType(command);
		this.source_ID = id;
		this.operation_TS = time;
		this.op_index = op_index;
	
		this.position = retrievePosition(command);
		
		if(this.operation_Type == 1 || this.operation_Type == 3)
			this.value = retrieveElement(command);
	}
	
	public Operation(byte [] op)
	{
		String op_buf = new String(op, StandardCharsets.UTF_8);
		String command = retrieveCommand(op_buf);

		this.operation_Type = retrieveOperationType(command);
		this.source_ID = retrieveID(op_buf);
		this.operation_TS = retrieveTS(op_buf);
		this.position = retrievePosition(command);
		this.op_index = retrieveOpIndex(op_buf);
				
		if(this.operation_Type == 1 || this.operation_Type == 3)
			this.value = retrieveElement(command);
	}
	
	public Operation(Operation ob)
	{
		this.operation_Type = ob.get_operationType();
		this.source_ID = ob.get_operationID();
		this.operation_TS = ob.get_operationTS();
		
		this.position = ob.getPosition();
		this.value = ob.getValue();
		this.op_index = ob.getOpIndex();
	}
		
	
	
	/************** getters *****************/
	public int get_operationType() 	{ return this.operation_Type; };
	
	public int get_operationID() 	{ return this.source_ID; };
	
	public long get_operationTS() 	{ return this.operation_TS; };
		
	public int getPosition() 		{ return this.position; };
	
	public String getValue() 		{ return this.value; };
	
	public int getOpIndex()			{ return this.op_index; };
	
	/************** setters *****************/
	
	public void setPosition(int pos) 	{ this.position = pos; };
	
	public void setIndex(int index) 	{ this.op_index = index; };
	
	//set NoOperation
	public void setNoOperation() 	{ this.operation_Type = 0;	};
	
	/************* Override ***************/
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Operation other = (Operation) obj;

		if (source_ID != other.source_ID)
			return false;
		if (operation_TS != other.operation_TS)
			return false;
		if (operation_Type != other.operation_Type)
			return false;
		if (op_index != other.op_index)
			return false;
		
		
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	};

	@Override
	public String toString() {
		
		//no operation case
		if(this.operation_Type == 0){
			return "[" + this.noop + "][" 
					+ this.source_ID + "][" + this.operation_TS + "][" + this.op_index + "]";
		};
		
		//insert case
		if(this.operation_Type == 1){
			return "[" + this.ins + "(" + this.position + "," + this.value + ")][" 
					+ this.source_ID + "][" + this.operation_TS + "][" + this.op_index + "]";
		};
		
		//delete case
		if(this.operation_Type == 2){
			return "[" + this.del + "(" + this.position + ")][" 
					+ this.source_ID + "][" + this.operation_TS + "][" + this.op_index + "]";
		};
				
		return "err_msg";
	};

	
	/************* operational***************/

	//Retrieving the command line from the operation [command][src_id][
	private String retrieveCommand(String operation)
	{		
		int position_start = 0;
		
		while(operation.charAt(position_start)!='[')		//here we can add more checkings
			position_start++;
		
		position_start++;
		
		int position_end = position_start;
		while(operation.charAt(position_end)!=']'){
			position_end++;
		}
				
		return operation.subSequence(position_start, position_end).toString();	
	};
	
	//Retrieving the operation source_ID
	private int retrieveID(String operation)
	{
		int position_start = 2;
		
		while(operation.charAt(position_start)!='['){		//here we can add more checkings
			position_start++;
		};
		
		position_start++;
		
		int position_end = position_start;
		while(operation.charAt(position_end)!=']'){
			position_end++;
		}
		
		return Integer.valueOf(operation.subSequence(position_start, position_end).toString());	
	};
	
	//Retrieving the TimeStamp of the operation
	private long retrieveTS(String operation)
	{
		int position_start = 0;
		int bracket = 0;
		
		while(true){
			position_start++;
			
			if(bracket==2)
				break;
			
			if(operation.charAt(position_start)=='[')
				bracket++;
		};
		
		int position_end = position_start;
		while(operation.charAt(position_end)!=']'){
			position_end++;
		}
			
		return Long.valueOf(operation.subSequence(position_start, position_end).toString());	
	}
	
	//Retrieving the operation type
	private int retrieveOperationType(String command)
	{
		if(command.contains("noOp"))
			return 0;
		
		if(command.contains("ins"))
			return 1;
		
		if(command.contains("del"))
			return 2;
		
		if(command.contains("upd"))
			return 3;
		
		return -1;
	};

	//Retrieving the position value from the command
	private int retrievePosition(String command)
	{		
		int position_start = 0;
		while(command.charAt(position_start)!='(')		//here we can add more checkings
			position_start++;
		
		position_start++;
		
		int position_end = position_start;
		while(command.charAt(position_end)!=','){
			
			if(command.charAt(position_end)==')')
				break;
			
			position_end++;
		}
				
		return Integer.parseInt(command.subSequence(position_start, position_end).toString());
	}
	
	private int retrieveOpIndex(String operation)
	{
		int position_start = 0;
		int bracket = 0;
		
		while(true){
			position_start++;
			
			if(bracket==3)
				break;
			
			if(operation.charAt(position_start)=='[')
				bracket++;
		};
		
		int position_end = position_start;
		while(operation.charAt(position_end)!=']'){
			position_end++;
		}
			
		return Integer.valueOf(operation.subSequence(position_start, position_end).toString());	
	}
	
	//Retrieving the element value from the command
	private String retrieveElement(String command){	
				
		int position_start = 0;
		while(command.charAt(position_start)!='(')
			position_start++;
		
		position_start++; //to pass a bracket.
		
		int position_end = position_start;
		
		while(command.charAt(position_end)!=',')	
			position_end++;
		
		position_end++;
		
		int element_end = position_end;
		while(command.charAt(element_end)!=')')
			element_end++;
	
		return command.subSequence(position_end, element_end).toString();
	}
	
};
