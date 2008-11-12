package net.gumbercules.loot;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.OrientationListener;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.MultiAutoCompleteTextView;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.MultiAutoCompleteTextView.Tokenizer;

public class TransactionActivity extends ListActivity
{
	public static final String KEY_REQ		= "t_req";
	public static final int ACTIVITY_CREATE	= 0;
	public static final int ACTIVITY_EDIT	= 1;
	public static final int ACTIVITY_DEL	= 2;
	
	public static final String KEY_TYPE		= "t_type";
	public static final int TRANSACTION		= 0;
	public static final int TRANSFER		= 1;
	
	public static final int NEW_TRANSACT_ID	= Menu.FIRST;
	public static final int NEW_TRANSFER_ID	= Menu.FIRST + 1;
	public static final int SORT_ID			= Menu.FIRST + 2;
	public static final int SEARCH_ID		= Menu.FIRST + 3;
	public static final int PURGE_ID		= Menu.FIRST + 4;
	public static final int SETTINGS_ID		= Menu.FIRST + 5;
	
	public static final int CONTEXT_EDIT	= Menu.FIRST;
	public static final int CONTEXT_COPY	= Menu.FIRST + 1;
	public static final int CONTEXT_DEL		= Menu.FIRST + 2;
	
	private ArrayList<Transaction> mTransList;
	private Account mAcct;
	
	private MultiAutoCompleteTextView searchEdit;
	
	private TextView budgetValue;
	private TextView balanceValue;
	private TextView postedValue;
	
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
    	super.onCreate(savedInstanceState);
    	setContentView(R.layout.main);
        
    	Bundle bun = getIntent().getExtras();
    	mAcct = Account.getAccountById(bun.getInt(Account.KEY_ID));
    	setTitle("loot :: " + mAcct.name);
    	
    	budgetValue = (TextView)findViewById(R.id.budgetValue);
    	balanceValue = (TextView)findViewById(R.id.balanceValue);
    	postedValue = (TextView)findViewById(R.id.postedValue);
    	
    	// add a listener to filter the list whenever the text changes
    	searchEdit = (MultiAutoCompleteTextView)findViewById(R.id.SearchEdit);
    	
    	// add a listener to clear searchEdit when pressed
    	ImageButton clearButton = (ImageButton)findViewById(R.id.ClearButton);
    	clearButton.setOnClickListener(new ImageButton.OnClickListener()
    	{
			public void onClick(View v)
			{
				searchEdit.setText("");
			}
    	});
    	
    	// find current orientation and send proper layout to constructor
    	int layoutResId = R.layout.trans_row_narrow;
    	if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
    		layoutResId = R.layout.trans_row_wide;
    	
    	mTransList = new ArrayList<Transaction>();
	    final TransactionAdapter ta = new TransactionAdapter(this, layoutResId, mTransList);
        setListAdapter(ta);
        fillList();
        
    	searchEdit.addTextChangedListener(new TextWatcher()
    	{
    		// we only care what the end result is
			public void afterTextChanged(Editable s)
			{
				ListView lv = ((ListActivity)ta.getContext()).getListView();
				lv.setTextFilterEnabled(true);
				lv.setFilterText(s.toString());
			}

			public void beforeTextChanged(CharSequence s, int start, int count, int after)
			{
				if (after == 0)
				{
					((ListActivity)ta.getContext()).getListView().clearTextFilter();
				}
			}

			public void onTextChanged(CharSequence s, int start, int before, int count) { }
    	});

    	ListView view = getListView();
        registerForContextMenu(view);
        
    	/*@SuppressWarnings("unused")
		OrientationListener orient = new OrientationListener(this)
    	{
			@Override
			public void onOrientationChanged(int orientation)
			{
				if (orientation >= 270 && orientation < 360)
					ta.setResource(R.layout.trans_row_wide);
				else
					ta.setResource(R.layout.trans_row_narrow);
			}
    	};*/
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
    	boolean result = super.onCreateOptionsMenu(menu);
    	menu.add(0, NEW_TRANSACT_ID, 0, R.string.new_trans)
    		.setIcon(android.R.drawable.ic_menu_add);
    	
    	// only show transfers if there is more than one account
    	if (Account.getAccountIds().length > 1)
    		menu.add(0, NEW_TRANSFER_ID, 0, R.string.transfer)
    			.setIcon(android.R.drawable.ic_menu_send);
    	
    	menu.add(0, SORT_ID, 0, R.string.sort)
    		.setIcon(android.R.drawable.ic_menu_sort_by_size);
    	menu.add(0, SEARCH_ID, 0, R.string.search)
    		.setIcon(android.R.drawable.ic_menu_search);
    	menu.add(0, PURGE_ID, 0, R.string.purge)
    		.setIcon(android.R.drawable.ic_menu_close_clear_cancel);
    	menu.add(0, SETTINGS_ID, 0, R.string.settings)
    		.setIcon(android.R.drawable.ic_menu_preferences);
    	
    	return result;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
    	switch (item.getItemId())
    	{
    	case NEW_TRANSACT_ID:
    		createTransaction();
    		return true;
    		
    	case NEW_TRANSFER_ID:
    		createTransfer();
    		return true;
    		
    	case SORT_ID:
    		sortDialog();
    		return true;
    		
    	case SEARCH_ID:
    		toggleSearch();
    		return true;
    		
    	case PURGE_ID:
    		purgeDialog();
    		return true;
    		
    	case SETTINGS_ID:
    		return true;
    	}
    	
    	return super.onOptionsItemSelected(item);
    }
    
    private void toggleSearch()
    {
		LinearLayout searchLayout = (LinearLayout)findViewById(R.id.SearchLayout);
		int new_vis = LinearLayout.VISIBLE;
		int cur_vis = searchLayout.getVisibility();
		
		// if it is currently visible, set it to gone
		if (new_vis == cur_vis)
		{
			new_vis = LinearLayout.GONE;
			searchEdit.setText("");
		}
		else
		{
			searchEdit.requestFocus();
			
			// set the adapter each time to get new data for the autocomplete
			String[] strings = Transaction.getAllStrings();
			if (strings == null)
				strings = new String[0];
			ArrayAdapter<String> searchAdapter = new ArrayAdapter<String>(this,
	    			android.R.layout.simple_dropdown_item_1line, strings);
			searchEdit.setAdapter(searchAdapter);
			searchEdit.setTokenizer(new SpaceTokenizer());
		}
		searchLayout.setVisibility(new_vis);
    }
    
    private void sortDialog()
    {
    	new AlertDialog.Builder(this)
    		.setTitle(R.string.sort_column)
    		.setItems(R.array.sort, new DialogInterface.OnClickListener()
    		{
    			public void onClick(DialogInterface dialog, int which)
    			{ 
    				if (which == 0)			// Date
    					Transaction.setComparator(Transaction.COMP_DATE);
    				else if (which == 1)	// Party
    					Transaction.setComparator(Transaction.COMP_PARTY);
    				else if (which == 2)	// Amount
    					Transaction.setComparator(Transaction.COMP_AMT);
    		    	((TransactionAdapter)getListAdapter()).sort();
    			}
    		})
    		.show();
    }

    private void purgeDialog()
    {
    	final Context context = (Context)this;
		AlertDialog dialog = new AlertDialog.Builder(this)
			.setTitle(R.string.account_del_box)
			.setItems(new CharSequence[]{"Purge Transactions", "Restore Purged Transactions",
					"Clear Purged Transactions"}, new DialogInterface.OnClickListener()
			{
				public void onClick(DialogInterface dialog, int which)
				{
					final int item = which;
					DatePickerDialog.OnDateSetListener dateSetListener =
				        new DatePickerDialog.OnDateSetListener()
						{
				            public void onDateSet(DatePicker view, int year, int month,  int day)
				            {
								Calendar cal = Calendar.getInstance();
								cal.set(Calendar.HOUR, 23);
								cal.set(Calendar.MINUTE, 59);
								cal.set(Calendar.SECOND, 59);
				            	cal.set(Calendar.YEAR, year);
				            	cal.set(Calendar.MONTH, month);
				            	cal.set(Calendar.DAY_OF_MONTH, day);
				            	Date date = cal.getTime();
				            	
				            	switch (item)
				            	{
				            	case 0:	// purge
				            		int[] purged = mAcct.purgeTransactions(date);
				            		if (purged != null)
				            		{
				            			for (int id : purged)
				            				updateList(id, ACTIVITY_DEL);
				            		}
				            		break;
				            		
				            	case 1:	// restore
				            		int[] restored = mAcct.restorePurgedTransactions(date);
				            		if (restored != null)
				            		{
				            			for (int id : restored)
				            				updateList(id, ACTIVITY_CREATE);
				            		}
				            		break;
				            		
				            	case 2:	// clear
				            		// TODO: this doesn't seem to work
				            		if (!mAcct.deletePurgedTransactions(date))
				            			Log.e("PURGE_DIALOG", "deleting purged transactions failed");
				            		break;
				            	}
				            }
				        };
				        
				    String title = "";
				    if (item == 0)
				    	title = "Purge Through";
				    else if (item == 1)
				    	title = "Restore Through";
				    else if (item == 2)
				    	title = "Clear Through";
				    
				    Calendar cal = Calendar.getInstance();
				    DatePickerDialog pickerDialog = new DatePickerDialog(context, dateSetListener,
				    		cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
				    pickerDialog.setTitle(title);
				    pickerDialog.show();
				}
			})
			.create();
		dialog.show();
    }
    
	private void createTransaction()
    {
    	Intent i = new Intent(this, TransactionEdit.class);
    	int request = ACTIVITY_CREATE;
    	i.putExtra(TransactionActivity.KEY_REQ, request);
    	i.putExtra(TransactionActivity.KEY_TYPE, TRANSACTION);
    	i.putExtra(Account.KEY_ID, mAcct.id());
    	startActivityForResult(i, request);    	
    }
    
    private void createTransfer()
    {
    	Intent i = new Intent(this, TransactionEdit.class);
    	int request = ACTIVITY_CREATE;
    	i.putExtra(TransactionActivity.KEY_REQ, request);
    	i.putExtra(TransactionActivity.KEY_TYPE, TRANSFER);
    	i.putExtra(Account.KEY_ID, mAcct.id());
    	startActivityForResult(i, request);
    }
    
    private void editTransaction(int id)
    {
    	Intent i = new Intent(this, TransactionEdit.class);
    	int request = ACTIVITY_EDIT;
    	i.putExtra(Transaction.KEY_ID, id);
    	i.putExtra(Account.KEY_ID, mAcct.id());
    	i.putExtra(TransactionActivity.KEY_REQ, request);
    	startActivityForResult(i, request);
    }
    
    public void setBalances()
    {
    	Double posted = mAcct.calculatePostedBalance();
    	Double balance = mAcct.calculateActualBalance();
    	Double budget = mAcct.calculateBudgetBalance();
    	
		// change the numbers to the locale currency format
		NumberFormat nf = NumberFormat.getCurrencyInstance();
		String str;
		
		if (posted != null)
			str = nf.format(posted);
		else
			str = "Error";
		postedValue.setText(str);
		
		if (balance != null)
			str = nf.format(balance);
		else
			str = "Error";
		balanceValue.setText(nf.format(balance));
		
		if (budget != null)
			str = nf.format(budget);
		else
			str = "Error";
		budgetValue.setText(nf.format(budget));
    }
    
    private void fillList()
    {
		int[] transIds = mAcct.getTransactionIds();
		ArrayList<Transaction> transList = mTransList;
		Transaction trans;
		transList.clear();
		
		if (transIds != null)
			for ( int id : transIds )
			{
				trans = Transaction.getTransactionById(id);
				if (trans != null)
					transList.add(trans);
			}
		Collections.sort(transList);
		
		setBalances();
    }
    
    private void updateList(int trans_id, int request)
    {
    	TransactionAdapter ta = (TransactionAdapter)getListAdapter();
    	Transaction trans;
    	int pos;
    	
    	switch (request)
    	{
    	case ACTIVITY_EDIT:
    		pos = ((TransactionAdapter)getListAdapter()).findItemById(trans_id);
    		ta.remove(ta.getItem(pos));
    		// don't break, the transaction needs to be added back to the list

    	case ACTIVITY_CREATE:
    		trans = Transaction.getTransactionById(trans_id);
    		ta.add(trans);
    		ta.sort();
    		break;
    		
    	case ACTIVITY_DEL:
    		pos = ((TransactionAdapter)getListAdapter()).findItemById(trans_id);
    		ta.remove(ta.getItem(pos));
    		break;
    	}

		setBalances();
    }

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
		
		if (resultCode == RESULT_OK && data != null)
		{
			Bundle extras = data.getExtras();
			updateList(extras.getInt(Transaction.KEY_ID), extras.getInt(TransactionActivity.KEY_REQ));
		}
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item)
	{
		AdapterView.AdapterContextMenuInfo info;
		try
		{
			info = (AdapterContextMenuInfo)item.getMenuInfo();
		}
		catch (ClassCastException e)
		{
			Log.e(TransactionActivity.class.toString(), "Bad ContextMenuInfo", e);
			return false;
		}
		
		int id = (int)getListAdapter().getItemId(info.position);
		switch (item.getItemId())
		{
		case CONTEXT_EDIT:
			editTransaction(id);
			return true;
			
		case CONTEXT_COPY:
			Transaction tr = Transaction.getTransactionById(id);
			tr.setId(-1);
			if (tr.type == Transaction.CHECK)
				tr.check_num = mAcct.getNextCheckNum();
			tr.post(false);
			id = tr.write(mAcct.id());
			updateList(id, ACTIVITY_CREATE);
			return true;
			
		case CONTEXT_DEL:
			final Transaction trans = Transaction.getTransactionById(id);
			AlertDialog dialog = new AlertDialog.Builder(this)
				.setTitle(R.string.account_del_box)
				.setMessage("Are you sure you wish to delete " + trans.party + "?")
				.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int which)
					{
						int id = trans.id();
						trans.erase();
						updateList(id, ACTIVITY_DEL);
					}
				})
				.setNegativeButton(R.string.no, new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int which) { }
				})
				.create();
			dialog.show();
			
			return true;
		}
		return false;
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
	{
		AdapterView.AdapterContextMenuInfo info;
		try
		{
			info = (AdapterContextMenuInfo)menuInfo;
		}
		catch (ClassCastException e)
		{
			Log.e(TransactionActivity.class.toString(), "Bad ContextMenuInfo", e);
			return;
		}
		
		if (info == null)
		{
			Log.e(TransactionActivity.class.toString(), "info == null");
			return;
		}

		Transaction trans = (Transaction)getListAdapter().getItem(info.position);
		if (trans == null)
			return;
		
		menu.setHeaderTitle(trans.party);
		
		menu.add(0, CONTEXT_EDIT, 0, R.string.edit);
		menu.add(0, CONTEXT_COPY, 0, R.string.copy);
		menu.add(0, CONTEXT_DEL, 0, R.string.del);
	}
	
	public static class SpaceTokenizer implements Tokenizer
	{
		public int findTokenEnd(CharSequence text, int cursor)
		{
			int i = cursor;
			int len = text.length();
			
			while (i < len)
				if (text.charAt(i) == ' ')
					return i;
				else
					++i;
			
			return len;
		}

		public int findTokenStart(CharSequence text, int cursor)
		{
			int i = cursor;
			
			while (i > 0 && text.charAt(i - 1) != ' ')
				--i;
			while (i < cursor && text.charAt(i) == ' ')
				++i;
			
			return i;
		}

		public CharSequence terminateToken(CharSequence text)
		{
			int i = text.length();
			
			while (i > 0 && text.charAt(i -1) == ' ')
				--i;
			
			if (i > 0 && text.charAt(i - 1) == ' ')
				return text;
			else
			{
				if (text instanceof Spanned)
				{
					SpannableString sp = new SpannableString(text + " ");
					TextUtils.copySpansFrom((Spanned)text, 0, text.length(), Object.class, sp, 0);
					return sp;
				}
				else
					return text + " ";
			}
		}
	}
}
