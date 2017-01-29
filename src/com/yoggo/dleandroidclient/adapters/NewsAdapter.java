package com.yoggo.dleandroidclient.adapters;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.text.Html;
import android.text.Html.ImageGetter;
import android.text.SpannableString;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.yoggo.dleandroidclient.FullNewsActivity;
import com.yoggo.dleandroidclient.R;
import com.yoggo.dleandroidclient.database.DaoMaster;
import com.yoggo.dleandroidclient.database.DaoSession;
import com.yoggo.dleandroidclient.database.DatabaseManager;
import com.yoggo.dleandroidclient.database.News;
import com.yoggo.dleandroidclient.database.NewsDao;
import com.yoggo.dleandroidclient.database.DaoMaster.DevOpenHelper;
import com.yoggo.dleandroidclient.interfaces.OldNewsLoadListener;
import com.yoggo.dleandroidclient.tools.SelectableMovementMethod;

public class NewsAdapter extends BaseAdapter {
	private Context context;
	private LayoutInflater inflater;
	private List<News> newsList;

	private OldNewsLoadListener newsLoadListener;
	private DatabaseManager dbManager;
	private boolean[] newsId;
	private boolean[] images;

	private DaoMaster daoMaster;
	private DaoSession daoSession;
	private SQLiteDatabase db;
	private NewsDao newsDao;

	// ��� �����
	public static final Map<String, WeakReference<Drawable>> mDrawableCache = Collections
			.synchronizedMap(new WeakHashMap<String, WeakReference<Drawable>>());

	public NewsAdapter(Context context, List<News> commentaries) {
		this.context = context;
		this.newsList = commentaries;
		inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		Log.d("news", "" + newsList.size());
		dbManager = new DatabaseManager(context);
		newsId = new boolean[newsList.size()];
		images = new boolean[newsList.size()];
		DevOpenHelper helper = new DaoMaster.DevOpenHelper(context,
				"onedle.db", null);
		db = helper.getWritableDatabase();
		daoMaster = new DaoMaster(db);
		daoSession = daoMaster.newSession();
		newsDao = daoSession.getNewsDao();
	}

	public void setOldNewsLoadListener(OldNewsLoadListener newsLoadListener) {
		this.newsLoadListener = newsLoadListener;
	}

	public void changeNewsId(List<News> newsList) {
		this.newsList = newsList;
		newsId = new boolean[newsList.size()];
		images = new boolean[newsList.size()];
		notifyDataSetChanged();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// ���������� ���������, �� �� ������������ view
		final ViewHolder holder;
		if (convertView == null) {
			holder = new ViewHolder();
			convertView = inflater.inflate(R.layout.content_layout, parent,
					false);
			holder.title = (TextView) convertView
					.findViewById(R.id.title_textView);
			holder.date = (TextView) convertView
					.findViewById(R.id.date_textView);
			holder.author = (TextView) convertView
					.findViewById(R.id.author_textView);
			holder.content = (TextView) convertView
					.findViewById(R.id.content_textView);
			holder.image = (ImageView) convertView
					.findViewById(R.id.imageView1);
			holder.category = (TextView) convertView
					.findViewById(R.id.category_textView);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}

		final News news = (News) getItem(position);

		holder.newsId = news.getId();
		// ������������� ��� �� ��������� - ������� �������
		holder.title.setTag(position);
		holder.title.setText(news.getTitle());
		holder.category.setText(dbManager.getCategoryNameById(news
				.getCategory()) + " | ");
		holder.shortContent = news.getShortStory();
		if (holder.shortContent == null) {
			holder.shortContent = "";
		}
		holder.formatShortContent = holder.shortContent;
		holder.link = new SpannableString("[...]");
		// ���� ������ 200 ������, �� ��������
		if (holder.shortContent.length() > 200
		// && newsId.length >= (Integer) holder.title.getTag()
				&& !newsId[(Integer) holder.title.getTag()]) {
			holder.formatShortContent = holder.shortContent.substring(0, 200);
			holder.link = new SpannableString("[...]");
		} else {
			holder.link = null;
		}

		// ���� ������ �������, ��������� ������ ������ (��� �������
		// actionbar'�)
		if (holder.title.getTag() != null
				&& (Integer) holder.title.getTag() == 0) {
			final TypedArray styledAttributes = context.getTheme()
					.obtainStyledAttributes(
							new int[] { android.R.attr.actionBarSize });
			int mActionBarSize = (int) styledAttributes.getDimension(0, 0);
			Log.d("actionbar height", "" + mActionBarSize);
			convertView.setPadding(0, mActionBarSize + dpToPx(15), 0, 0);
		} else {
			convertView.setPadding(0, 0, 0, 0);
		}

		// �������� �� ��������� �������, ���� ��������� - ��������� ���
		if (holder.title.getTag() != null
				&& (Integer) holder.title.getTag() == newsList.size() - 1) {
			// ��������� ������� �� ��������� �������
			if (newsLoadListener != null) {
				newsLoadListener.loadNews();
			}
		}

		// ���� ������ 200 ������ �� ��������� ������ [...] � �����
		if (holder.shortContent.length() > 200
		// && newsId.length >= (Integer) holder.title.getTag()
				&& !newsId[(Integer) holder.title.getTag()]) {
			ClickableSpan clickableSpan = new ClickableSpan() {
				@Override
				public void onClick(View textView) {
					// ���������� short ������� �������
					holder.content.setText(Html.fromHtml(holder.shortContent,
							holder.igLoader, null));
					// ������ � ������ ������� ��� ��� ������� ��������
					newsId[(Integer) holder.title.getTag()] = true;
				}

				@Override
				public void updateDrawState(TextPaint tp) {
					// ������� �������������
					tp.setUnderlineText(false);
				}
			};
			// ������������� ����
			holder.link.setSpan(clickableSpan, 0, holder.link.length(), 0);
		}

		if (news.getImage() != null) {
			Bitmap btm = BitmapFactory.decodeByteArray(news.getImage(), 0,
					news.getImage().length);
			holder.bitmap = btm;
		}

		// ������ ��������
		holder.igLoader = new Html.ImageGetter() {
			public Drawable getDrawable(String source) {
				images[(Integer) holder.title.getTag()] = true;
				// ���� ������� ���������� � ����, �� ������ ������������� ��� �
				// ������ �� ������ ������
				if (mDrawableCache.containsKey(source)) {
					holder.drawable = mDrawableCache.get(source).get();
					holder.image.setImageDrawable(holder.drawable);
					return new BitmapDrawable(context.getResources());
				} else {
					// � ��������� ������, ��������� ��� �� ����
					new ImageDownloadAsyncTask(source,
							holder.formatShortContent, holder, holder.link,
							images[(Integer) holder.title.getTag()], news)
							.execute();
					// ���� �� ����������� ������������� ������ �������
					Drawable color = context.getResources().getDrawable(
							R.drawable.gray_rectangle);
					holder.drawable = color;
					holder.image.setImageDrawable(color);
				}
				return new BitmapDrawable(context.getResources());
			}
		};

		try {
			// ������ ����� (� ���������)
			holder.content.setText(Html.fromHtml(holder.formatShortContent,
					holder.igLoader, null));
		} catch (Exception e) {
			e.printStackTrace();
		}
		// ��������� �� ������� - ������� � ��������� ��� ���
		if (images[(Integer) holder.title.getTag()]) {
			holder.image.setVisibility(View.VISIBLE);
			if (holder.bitmap != null) {
				holder.image.setImageBitmap(holder.bitmap);
			} else if (holder.drawable != null) {
				holder.image.setImageDrawable(holder.drawable);
			}
		} else {
			holder.image.setVisibility(View.GONE);
			holder.image.setImageDrawable(null);
		}

		// ���� �� null, �� ��������� ������ [...] � �����
		if (holder.link != null) {
			holder.content.append(holder.link);
			holder.content.setMovementMethod(new SelectableMovementMethod());
		}

		// ��������� ����
		holder.date.setText(news.getDate());
		holder.author.setText(news.getAuthor() + " | ");

		// listener ��� ���� �� ������� - ��������� ������ �������
		convertView.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = new Intent(context, FullNewsActivity.class);
				intent.putExtra(FullNewsActivity.NEWS_ID,
						String.valueOf(holder.newsId));
				context.startActivity(intent);
			}

		});
		return convertView;
	}

	public static byte[] getBitmapAsByteArray(Bitmap bitmap) {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		bitmap.compress(CompressFormat.PNG, 0, outputStream);
		return outputStream.toByteArray();
	}

	/*
	 * ����� ��� ����������� �������� ��������
	 */
	class ImageDownloadAsyncTask extends AsyncTask<Void, Void, Void> {
		private String source;
		private String message;
		private ViewHolder holder;
		boolean isImage;
		private News news;

		public ImageDownloadAsyncTask(String source, String message,
				ViewHolder holder, SpannableString spannable, boolean isImage,
				News news) {
			this.source = source;
			this.message = message;
			this.holder = holder;
			this.isImage = isImage;
			this.news = news;
			Log.d("source", source);
		}

		@Override
		protected Void doInBackground(Void... params) {
			if (!mDrawableCache.containsKey(source)) {
				try {
					// ��������� �������� � ��� ���
					URL url = new URL(source);
					URLConnection connection = url.openConnection();
					InputStream is = connection.getInputStream();

					BitmapDrawable drawable = (BitmapDrawable) Drawable
							.createFromStream(is, "src");
					/*
					 * Bitmap bmp = BitmapFactory.decodeStream(is);
					 * DisplayMetrics dm =
					 * context.getResources().getDisplayMetrics();
					 * bmp.setDensity(dm.densityDpi); Drawable drawable=new
					 * BitmapDrawable(context.getResources(),bmp);
					 */
					is.close();

					drawable.setBounds(0, 0, drawable.getIntrinsicWidth(),
							drawable.getIntrinsicHeight());

					byte[] byteArray = getBitmapAsByteArray(drawable
							.getBitmap());

					news.setImage(byteArray);
					// �� ���������
					newsDao.insertOrReplace(news);

					mDrawableCache.put(source, new WeakReference<Drawable>(
							drawable));
				} catch (MalformedURLException e) {
					e.printStackTrace();
				} catch (Throwable t) {
					t.printStackTrace();
				}
			}
			return null;
		}

		Html.ImageGetter igCached = new Html.ImageGetter() {
			public Drawable getDrawable(String source) {
				// ������ ���������� ��� ������� �� ����
				if (mDrawableCache.containsKey(source)) {
					holder.drawable = mDrawableCache.get(source).get();

					holder.image.setImageDrawable(holder.drawable);

					return new BitmapDrawable(context.getResources());
				}
				return new BitmapDrawable(context.getResources());
			}
		};

		@Override
		protected void onPostExecute(Void result) {
			// ����������������� ���������� ������ ����
			if (isImage == images[(Integer) holder.title.getTag()]) {
				holder.content.setText(Html.fromHtml(message, igCached, null));
				images[(Integer) holder.title.getTag()] = true;
				if (holder.link != null) {
					holder.content.append(holder.link);
					holder.content.setMovementMethod(LinkMovementMethod
							.getInstance());
				}
			}
		}
	}

	class ViewHolder {
		TextView title;
		TextView date;
		TextView author;
		TextView content;
		TextView category;
		ImageView image;
		Drawable drawable;
		Bitmap bitmap;
		long newsId;
		String shortContent;
		String formatShortContent;
		boolean isShowAll;
		ImageButton button;
		SpannableString link;
		ImageGetter igLoader;
	}

	@Override
	public int getCount() {
		return newsList.size();
	}

	// ������� �� �������
	@Override
	public Object getItem(int position) {
		return newsList.get(position);
	}

	// id �� �������
	@Override
	public long getItemId(int position) {
		return position;
	}

	public static int dpToPx(int dp) {
		return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
	}
}
