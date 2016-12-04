package com.septianfujianto.woodroid.Products;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.MenuInflater;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.septianfujianto.woodroid.Config;
import com.septianfujianto.woodroid.Model.Products.Product;
import com.septianfujianto.woodroid.Model.Realm.RealmHelper;
import com.septianfujianto.woodroid.Products.UI.ProductsAdapter;
import com.septianfujianto.woodroid.R;
import com.septianfujianto.woodroid.Services.IWoocommerceServices;
import com.septianfujianto.woodroid.ShoppingCart.ShoppingCart;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import id.gits.baso.BasoProgressView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ProductsActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    private RecyclerView mProductRcv;
    private Context mContext;
    private List<Product> listProducts;
    private ProductsAdapter adapter;
    protected BasoProgressView basoProgressView;
    protected GridLayoutManager layoutManager;
    int init_page = 1, per_page = 4, run_counter= 0;
    protected boolean loadMoreProduct = true;
    protected IWoocommerceServices service;
    protected SearchView searchView;
    protected Toolbar toolbar;
    protected SearchManager searchManager;
    protected Map<String, Object> options = new HashMap<>();
    protected Map<String, Object> parameters = new HashMap<>();
    private FloatingActionButton fab;
    private RealmHelper helper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_products);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("Latest Product");
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        fab = (FloatingActionButton) findViewById(R.id.fab);
        helper = new RealmHelper(this);

        if (helper.getAllCartItems().size() <= 0) {
            fab.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));
        } else {
            fab.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.colorAccent)));
        }

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(mContext, ShoppingCart.class));
            }
        });

        basoProgressView = (BasoProgressView) findViewById(R.id.baso_ProgressView);
        basoProgressView.startProgress();

        mContext = this;
        mProductRcv = (RecyclerView) findViewById(R.id.rcv_product);
        listProducts = new ArrayList<>();

        adapter = new ProductsAdapter(this, listProducts);

        mProductRcv.setHasFixedSize(true);
        layoutManager = new GridLayoutManager(this, 2);
        mProductRcv.setLayoutManager(layoutManager);
        mProductRcv.setAdapter(adapter);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(Config.SITE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        service = retrofit.create(IWoocommerceServices.class);

        handleSearchIntent(getIntent());
    }

    public void loadProducts(int page) {
        options.put("options[wp_api]", true);
        options.put("options[version]", "wc/v1");

        parameters.put("parameters[page]", init_page);
        parameters.put("parameters[per_page]", per_page);

        Call<List<Product>> call = service.getAllItems(
                Config.SITE_URL, Config.COSTUMER_KEY, Config.COSTUMER_SECRET,
                options, "products/", parameters
        );

        call.enqueue(new Callback<List<Product>>() {
            @Override
            public void onResponse(Call<List<Product>> call, Response<List<Product>> response) {
                basoProgressView.stopAndGone();
                Log.e(" Response RAW ",String.valueOf(response.raw()));

                if (response.isSuccessful()) {
                    listProducts.addAll(response.body());
                    adapter.notifyDataChanged();
                } else {
                    basoProgressView.stopAndError("Oops, something Wrong: "+String.valueOf(response.code()));
                    Log.e("Response Error ", String.valueOf(response.code()));
                }
            }

            @Override
            public void onFailure(Call<List<Product>> call, Throwable t) {
                basoProgressView.stopAndError(t.getMessage());
                t.printStackTrace();
            }
        });
    }

    public void loadMoreProducts(int page) {
        options.put("options[wp_api]", true);
        options.put("options[version]", "wc/v1");

        parameters.put("parameters[page]", page);
        parameters.put("parameters[per_page]", per_page);

        Call<List<Product>> call = service.getAllItems(
                Config.SITE_URL, Config.COSTUMER_KEY, Config.COSTUMER_SECRET,
                options, "products/", parameters
        );

        call.enqueue(new Callback<List<Product>>() {
            @Override
            public void onResponse(Call<List<Product>> call, Response<List<Product>> response) {
                if(response.isSuccessful()) {
                    List<Product> result = response.body();

                    if(result.size() > 0) {
                        listProducts.addAll(result);
                    } else {
                        adapter.setMoreDataAvailable(false);
                        Toast.makeText(mContext,"No More Data Available",Toast.LENGTH_SHORT).show();
                    }

                    adapter.notifyDataChanged();
                } else {
                    Log.e("Response Error", String.valueOf(response.code()));
                }
            }

            @Override
            public void onFailure(Call<List<Product>> call, Throwable t) {
                t.printStackTrace();
            }
        });
    }

    private void searchProducts(String query) {
        options.put("options[wp_api]", true);
        options.put("options[version]", "wc/v1");

        parameters.put("parameters[page]", init_page);
        parameters.put("parameters[per_page]", per_page);
        parameters.put("parameters[search]", query);

        Call<List<Product>> call = service.getAllItems(
                Config.SITE_URL, Config.COSTUMER_KEY, Config.COSTUMER_SECRET,
                options, "products/", parameters
        );

        call.enqueue(new Callback<List<Product>>() {
            @Override
            public void onResponse(Call<List<Product>> call, Response<List<Product>> response) {
                Log.e(" Response  ", response.raw().toString());
                basoProgressView.stopAndGone();
                if (response.isSuccessful()) {
                    //clearData();
                    List<Product> result = response.body();

                    if(result.size() > 0) {
                        listProducts.addAll(result);
                    } else {
                        adapter.setMoreDataAvailable(false);
                        Toast.makeText(mContext,"No More Data Available",Toast.LENGTH_SHORT).show();
                    }

                    adapter.notifyDataChanged();
                    searchManager.stopSearch();
                } else {
                    Log.e(" Response Error ",String.valueOf(response.code()));
                }
            }

            @Override
            public void onFailure(Call<List<Product>> call, Throwable t) {
                t.printStackTrace();
                Log.e(" Response Error ",t.getMessage().toString());
            }
        });
    }

    public void clearData() {
        int size = listProducts.size();
        if (size > 0) {
            for (int i = 0; i < size; i++) {
                listProducts.remove(0);
            }

            adapter.notifyItemRangeRemoved(0, size);
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        handleSearchIntent(intent);
    }

    private void handleSearchIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);

            if (query.length() > 3) {
                Toast.makeText(mContext, "Searching for "+query, Toast.LENGTH_SHORT).show();
                toolbar.setTitle("Searching for "+query);
                clearData();
                searchProducts(query);
            }

        } else {
            initProductsLoad();
        }
    }

    private void initProductsLoad() {
        // Load the first batch of products
        loadProducts(init_page);

        // Load next products batch when scrolled
        adapter.setLoadMoreListener(new ProductsAdapter.OnLoadMoreListener() {
            @Override
            public void onLoadMore() {
                mProductRcv.post(new Runnable() {
                    @Override
                    public void run() {
                        int counter = run_counter++;
                        Toast.makeText(mContext, "LOADMORE Batch "+counter, Toast.LENGTH_SHORT).show();

                        loadMoreProducts(1+init_page++);
                    }
                });
            }
        });
    }

    private void retryClicked() {
        basoProgressView.setOnButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                startActivity(getIntent());
                Toast.makeText(v.getContext(), "Retry CLICKED", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public String encodeUrl(String url) {
        String encodedurl = "";
        try {

            encodedurl = URLEncoder.encode(url,"UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return encodedurl;
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.products, menu);

        // Associate searchable configuration with the SearchView
        searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        searchView.setSearchableInfo(
                searchManager.getSearchableInfo(getComponentName()));

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_search) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}