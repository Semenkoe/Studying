
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.swirlds.platform.Address;
import com.swirlds.platform.AddressBook;
import com.swirlds.platform.FCDataInputStream;
import com.swirlds.platform.FCDataOutputStream;
import com.swirlds.platform.FastCopyable;
import com.swirlds.platform.Platform;
import com.swirlds.platform.SwirldState;

/**
 * This holds the current state of the swirld platform app. 
 */
public class HelloSwirldDemoState implements SwirldState {
	
	//Global operation log
	private List<Operation>	operations	= Collections.synchronizedList(new ArrayList<Operation>());
	
	//have no idea what is inside of this...
	private AddressBook		addressBook;

	//get Operation (for each client)
	public synchronized Operation getOperation(int index) {
				
		try{
			return operations.get(index);
		}catch(IndexOutOfBoundsException e)
		{	
			return null;
		}
	};
	
	public static Operation transformation(Operation o1, Operation o2)
	{
		//no operation					- type 0
		//ins(position,element);		- type 1
		//del(position);				- type 2
		
		//fisrt we need to check NoOp existing
		if(o1.get_operationType()==0 || o2.get_operationType()==0)
			return o1;
				
		
		/*
		 *  T(Ins(p1,c1,u1),Ins(p2,c2,u2)) : - 
		 *	if (p1 < p2) or (p1 == p2 and u1 < u2) return Ins(p1 ,c1 ,u1)
		 *	else return Ins(p1 + 1,c1,u1)
		 * 
		 *  p - position
		 *  c - character
		 *  u - user ID (priority?)
		 */
		
		if(o1.get_operationType()==1 && o2.get_operationType()==1)
		{
			if(	o1.getPosition()<o2.getPosition() || 
				(o1.getPosition() == o2.getPosition() && o1.get_operationTS() < o2.get_operationTS()) )
				return o1;
			else
			{
				o1.setPosition(o1.getPosition()+1);
				return o1;
			}
		};
		
		
		/*
		 * T(Ins(p1 ,c1 ,u1),Del(p2 ,u2 )) : -
		 * if( p1 <= p2) return Ins(p1 ,c1 ,u1)
		 * else return Ins(p1 - 1,c1,u1)
		 * 
		 */
		
		if(o1.get_operationType()==1 && o2.get_operationType()==2)
		{
			if(o1.getPosition() <= o2.getPosition())
				return o1;
			else
			{
				o1.setPosition(o1.getPosition()-1);
				return o1;
			}
		};
		
		
		/*
		 * T(Del(p1,u1),Ins(p2,c2,u2)): -
		 * if(p1 < p2) return Del(p1,u1)
		 * else return Del(p1 + 1,u1)
		 * 
		 */
		
		if(o1.get_operationType()==2 && o2.get_operationType()==1)
		{
			if(o1.getPosition()<o2.getPosition())
				return o1;
			else
			{
				o1.setPosition(o1.getPosition()+1);
				return o1;
			}
		};
		
		/*
		 * T(Del(p1 ,u1),Del(p2 ,u2)) : -
		 * if ( p1 < p2 ) return Del(p1 ,u1)
		 * else if ( p1 > p2) return Del(p1 - 1,u1)
		 * else return Id()
		 * 
		 */
		
		if(o1.get_operationType()==2 && o2.get_operationType()==2)
		{
			if(o1.getPosition()<o2.getPosition())
				return o1;
			else
				if(o1.getPosition()>o2.getPosition())
				{
					o1.setPosition(o1.getPosition()-1);
					return o1;
				}
				else {
					
					//changing type to NoOp
					o1.setNoOperation();
					return o1;
				}
		};
		
		//this state is impossible... but still.
		return null;
	};

	/**************************************************************/

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized void handleTransaction(long id, boolean consensus,
			Instant timeCreated, byte[] transaction, Address address) 
	{
						
		Operation obj = new Operation(transaction);
		
		if(obj.getOpIndex()<this.operations.size())
		{
			for(int i = obj.getOpIndex(); i<this.operations.size(); i++)
			{
				if( (obj.get_operationID()!=this.operations.get(i).get_operationID()))// && this.operations.get(i).transformed==true)
						obj = transformation(obj, this.operations.get(i));
			}
		};
		
		this.operations.add(obj);
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized AddressBook getAddressBookCopy() {
		return addressBook.copy();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized FastCopyable copy() {
		HelloSwirldDemoState copy = new HelloSwirldDemoState();
		copy.copyFrom(this);
		return copy;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void copyTo(FCDataOutputStream outStream) {
		System.out.println("copyTo _ FCDataOutputStream");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void copyFrom(FCDataInputStream inStream) {
		System.out.println("copyFrom _ FCDataInputStream");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized void copyFrom(SwirldState old) {
		operations = Collections.synchronizedList(
				new ArrayList<Operation>(((HelloSwirldDemoState) old).operations));
		addressBook = ((HelloSwirldDemoState) old).addressBook.copy();
		
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void freeze() {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized void init(Platform platform, AddressBook addressBook) {
		this.addressBook = addressBook;
	}
	
}