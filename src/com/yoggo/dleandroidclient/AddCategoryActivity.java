package com.yoggo.dleandroidclient;

import java.util.List;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.yoggo.dleandroidclient.account.AccountSettingsManager;
import com.yoggo.dleandroidclient.database.Categories;
import com.yoggo.dleandroidclient.database.CategoriesDao;
import com.yoggo.dleandroidclient.database.DaoMaster;
import com.yoggo.dleandroidclient.database.DatabaseManager;
import com.yoggo.dleandroidclient.database.DaoMaster.DevOpenHelper;
import com.yoggo.dleandroidclient.database.DaoSession;
import com.yoggo.dleandroidclient.json.AddCategoryJson;
import com.yoggo.dleandroidclient.requests.AddCategory;
import com.yoggo.dleandroidclient.requests.EditCategory;

public class AddCategoryActivity extends ActionBarActivity implements OnClickListener{
	
	//��������� ��� �������� �������� � intent
	public static final String IS_EDIT = "IS_EDIT";
	public static final String CATEGORY_ID = "CATEGORY_ID";
	public static final String CATEGORY_NAME = "CATEGORY_NAME";
	public static final String CATEGORY_ALT_NAME = "CATEGORY_ALT_NAME";
	public static final String CATEGORY_PARENT_ID = "CATEGORY_PARENT_ID";
	
	
	private EditText nameEditText;
	private EditText altNameEditText;
	private Spinner categoriesSpinner;
	private Button sendButton;
	
	private String[] catNames;
	private boolean isEdit;
	private String categoryId;
	
	SQLiteDatabase db;
	DaoMaster daoMaster;
    DaoSession daoSession;
    DevOpenHelper helper;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_add_category);
		categoriesSpinner = (Spinner) findViewById(R.id.add_category_spinner);
		nameEditText = (EditText) findViewById(R.id.add_category_name_editText);
		altNameEditText = (EditText) findViewById(R.id.add_category_alt_name_editText);
		sendButton = (Button) findViewById(R.id.add_category_send_button);
		sendButton.setOnClickListener(this);
		
		helper = new DaoMaster.DevOpenHelper(getApplicationContext(), DatabaseManager.DATABASE_NAME, null);
		db = helper.getWritableDatabase();
        daoMaster = new DaoMaster(db);
        daoSession = daoMaster.newSession();
        
		getCategories();
		setCategorySpinner();
		Intent intent = getIntent();
		isEdit(intent);
	}
	
	/*
	 * ������������� ������� � �����������
	 * */
	private void setCategorySpinner(){
		ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, catNames);
		spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		categoriesSpinner.setAdapter(spinnerAdapter);
	}
	
	/*
	 * ����� ��� ����������� - ��������� ����� ��� ����������� ������������ ���������
	 * */
	private void isEdit(Intent intent){
		isEdit = intent.getBooleanExtra(IS_EDIT, false);
		if(isEdit){
			sendButton.setText(getResources().getString(R.string.edit));
			setTitle(getResources().getString(R.string.edit_category));
			nameEditText.setText(intent.getStringExtra(CATEGORY_NAME));
			altNameEditText.setText(intent.getStringExtra(CATEGORY_ALT_NAME));
			categoryId = intent.getStringExtra(CATEGORY_ID);
			String catName = getCategoryNameFromId(intent.getStringExtra(CATEGORY_PARENT_ID));
			int pos = 0;
			//�������� ������������ ��������� (���� ����)
			for(int i = 0; i <catNames.length; i++){
				if(catNames[i].equals(catName)){
					pos = i;
					break;
				}
			}
			categoriesSpinner.setSelection(pos);
		}else{
			setTitle(getResources().getString(R.string.add_category));
		}
	}
	
	@Override
	public void onClick(View v){
		switch(v.getId()){
		case R.id.add_category_send_button:
			if(isEdit){
				editCategory();
			}else{
				addCategory();
			}
			break;
		}
	}
	
	/*
	 * �������� ��� ���������
	 * */
	private void getCategories(){
		CategoriesDao catDao = daoSession.getCategoriesDao();
		List<Categories> cats = catDao.loadAll();
		catNames = new String[cats.size()+1];
		//������ ��������� ��������� ������ (��� ������������ ���������)
		catNames[0] = "";
		for(int i = 0; i< cats.size() ; i++){
			catNames[i+1] = cats.get(i).getName();
		}
		
	}
	
	/*
	 * �������� id ��������� �� �����
	 * */
	private String getCategoryIdFromName(){
		CategoriesDao catDao = daoSession.getCategoriesDao();
		if(((String)categoriesSpinner.getSelectedItem()).equals("0")){
			return null;
		}
		List<Categories> list = catDao.queryBuilder().where(CategoriesDao.Properties.Name.eq((String)categoriesSpinner.getSelectedItem())).limit(1).list();
		String catList = "";
		for(Categories cat : list){
			catList += cat.getId();
		}
		return catList;
	}
	
	/*
	 * �������� ��� ��������� �� id
	 * */
	private String getCategoryNameFromId(String id){
		CategoriesDao catDao = daoSession.getCategoriesDao();
		List<Categories> list = catDao.queryBuilder().where(CategoriesDao.Properties.Id.eq(id)).limit(1).list();
		String catList = "";
		for(Categories cat : list){
			catList = cat.getName();
		}
		return catList;
	}
	
	/*
	 * ����� ���������� ���������
	 * */
	private void addCategory(){
    	AccountSettingsManager account = new AccountSettingsManager(
				this);
		Callback<AddCategoryJson> callback = new Callback<AddCategoryJson>() {

			@Override
			public void failure(RetrofitError arg0) {
				Toast.makeText(getApplicationContext(), "������ ��� ���������� ���������",
				 Toast.LENGTH_SHORT).show();
				Log.d("ERROR", arg0.getMessage());

			}

			@Override
			public void success(AddCategoryJson list, Response arg1) {
				if (list != null) {
					if(list.result != null){
						Log.d("result", list.result);
					}
					if(list.error != null){
						Log.d("error", list.error);
						Toast.makeText(getApplicationContext(),
								list.error, Toast.LENGTH_SHORT).show();
						return;
					}
					//���� �������
					Toast.makeText(getApplicationContext(),
							"��������� ���������", Toast.LENGTH_SHORT).show();
					finish();
				} else {
					Toast.makeText(getApplicationContext(),
							"������ ��� ���������� ���������", 200).show();
				}
			}

		};
		//�������� id ������������ ���������
		String parentId = getCategoryIdFromName();
		if(parentId != null){
			//������ � parentId
			new AddCategory(account.getSite(), 
					account.getToken(), 
					nameEditText.getText().toString(),
					altNameEditText.getText().toString(),
					parentId,
					callback);
		}else{
			//������ ��� parentId
			new AddCategory(account.getSite(), 
					account.getToken(), 
					nameEditText.getText().toString(),
					altNameEditText.getText().toString(),
					callback);
		}
			
    }
	
	/*
	 * ����� �������������� ���������
	 * */
	private void editCategory(){
    	AccountSettingsManager account = new AccountSettingsManager(
				this);
		Callback<AddCategoryJson> callback = new Callback<AddCategoryJson>() {

			@Override
			public void failure(RetrofitError arg0) {
				Toast.makeText(getApplicationContext(), "������ ��� ��������� ���������",
				 Toast.LENGTH_SHORT).show();
				Log.d("ERROR", arg0.getMessage());

			}

			@Override
			public void success(AddCategoryJson list, Response arg1) {
				if (list != null) {
					if(list.result != null){
						Log.d("result", list.result);
					}
					if(list.error != null){
						Log.d("error", list.error);
						Toast.makeText(getApplicationContext(),
								list.error, 200).show();
						return;
					}
					//���� �������
					Toast.makeText(getApplicationContext(),
							"��������� ��������", 200).show();
					finish();
				} else {
					Toast.makeText(getApplicationContext(),
							"������ ��� ��������� ���������", 200).show();
				}
			}

		};
		//�������� id ������������ ���������
		String parentId = getCategoryIdFromName();
		if(parentId != null){
			//������ � parentId
			new EditCategory(account.getSite(), 
					account.getToken(), 
					nameEditText.getText().toString(),
					altNameEditText.getText().toString(),
					categoryId,
					parentId,
					callback);
		}else{
			//������ ��� parentId
			new EditCategory(account.getSite(), 
					account.getToken(), 
					nameEditText.getText().toString(),
					altNameEditText.getText().toString(),
					categoryId,
					callback);
		}
			
    }
}
