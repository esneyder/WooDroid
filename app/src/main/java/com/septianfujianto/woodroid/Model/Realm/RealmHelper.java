package com.septianfujianto.woodroid.Model.Realm;

import android.content.Context;
import android.widget.Toast;

import com.septianfujianto.woodroid.Config;
import com.septianfujianto.woodroid.Utils.Utils;
import com.woocommerse.OAuth1.services.TimestampServiceImpl;

import java.util.ArrayList;

import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;

import static com.septianfujianto.woodroid.R.id.totalPrices;

/**
 * Created by Septian A. Fujianto on 12/1/2016.
 */

public class RealmHelper {
    private Realm realm;
    private RealmResults<Cart> cartResults;
    private Cart cart;
    public Context mContext;

    public RealmHelper(Context mContext) {
        realm = Realm.getDefaultInstance();
        this.mContext = mContext;
    }

    public void addItemToCart(String cartId, String customerId, int productId, String productName, int productQty, Double productPrice, String productImage) {
        cart = new Cart();

        cart.setCart(cartId, customerId, productId, productName, productQty, productPrice);
        cart.setProductImage(productImage);
        realm.beginTransaction();
        realm.copyToRealmOrUpdate(cart);
        realm.commitTransaction();
    }

    public RealmResults<Cart> getAllCartItems() {
        cartResults = realm.where(Cart.class).findAll();
        cartResults.sort("productId", Sort.DESCENDING);

        return cartResults;
    }

    public RealmResults<Cart> getCartItemsByProductId(int productId) {
        cartResults = realm.where(Cart.class).equalTo("productId", productId).findAll();
        cartResults.sort("productId", Sort.DESCENDING);

        if(cartResults.size() != 0) {
            return cartResults;
        } else {
            return null;
        }
    }

    public void updateCartItemByProductId(int productId, String productName, int productQty, Double productPrice) {
        cart = realm.where(Cart.class).equalTo("productId", productId).findFirst();
        realm.beginTransaction();

        if (productName != null) {
            cart.setProductName(productName);
        }

        if (productQty > 0) {
            cart.setProductQty(productQty);
        }

        if (productPrice != null) {
            cart.setProductPrice(productPrice);
        }

        realm.copyToRealmOrUpdate(cart);
        realm.commitTransaction();
    }

    public void deleteCartItemByProductId(int productId) {
        RealmResults<Cart> dataResults = realm.where(Cart.class).equalTo("productId", productId).findAll();
        realm.beginTransaction();
        dataResults.deleteFirstFromRealm();

        realm.commitTransaction();
    }

    public void deleteAllCartItem() {
        RealmResults<Cart> dataResults = realm.where(Cart.class).findAll();
        realm.beginTransaction();
        dataResults.deleteAllFromRealm();
        realm.commitTransaction();
    }

    public Double getCartTotalAmount(){
        RealmResults<Cart> realmResults = realm.where(Cart.class).findAll();
        if (realmResults.size() > 0) {
            ArrayList<Double> subtotal = new ArrayList<>();

            for (int i = 0; i < realmResults.size(); i++) {
                subtotal.add(Double.valueOf(realmResults.get(i).getProductPrice() * realmResults.get(i).getProductQty()));
            }

            return Utils.sumList(subtotal);

        } else {
            return Double.valueOf(0);
        }
    }
}
