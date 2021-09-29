package com.example.cmpt_cobalt.view;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.cmpt_cobalt.R;
import com.example.cmpt_cobalt.model.Inspection;
import com.example.cmpt_cobalt.model.ParseCSV;
import com.example.cmpt_cobalt.model.Restaurant;
import com.example.cmpt_cobalt.model.RestaurantManager;
import com.google.gson.Gson;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// main screen activity
// displays the initial list of restaurants
public class MainActivity extends AppCompatActivity {

    SharedPreferences mSharedPreferences;

    private static final String EXTRA_MESSAGE = "Extra";
    private RestaurantManager manager;
    private int size = 0;
    private String[] restaurantStrings = new String[size];

    List<Restaurant> restaurants = new ArrayList<>();
    List<Restaurant> updatedRestaurants = new ArrayList<>();

    public static Intent makeLaunchIntent(Context c, String message) {
        Intent i1 = new Intent(c, MainActivity.class);
        i1.putExtra(EXTRA_MESSAGE, message);
        return i1;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        try {
            populateListView();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        compareForUpdate();

        registerClickCallback();
    }

    private void compareForUpdate() {
        manager = RestaurantManager.getInstance();
        mSharedPreferences = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE);
        Set<String> favourites = new HashSet<String>(mSharedPreferences.getStringSet("Favourites", new HashSet<String>()));

        ArrayList<String> toRemove = new ArrayList<>();
        ArrayList<String> toAdd = new ArrayList<>();

        for (String OldJson : favourites) {
            for (Restaurant newRes : manager) {
                if (newRes.getFavourite()) {
                    System.out.println("Test> Evaluating " + newRes.toString() + " Favourite: " + newRes.getFavourite());
                    Gson gson = new Gson();
                    Restaurant oldRes = gson.fromJson(OldJson, Restaurant.class);
                    System.out.println("Test> Comparing with " + newRes.toString() + " Favourite: " + newRes.getFavourite());

                    String newJson = new Gson().toJson(newRes);
                    if (oldRes.getTracking().equals(newRes.getTracking())) {
                        if (!OldJson.equals(newJson)) {
                            updatedRestaurants.add(newRes);
                            System.out.println("Test> Adding " + newRes.toString() + " Favourite: " + newRes.getFavourite());
                            toRemove.add(OldJson);
                            toAdd.add(newJson);
                        }
                    }
                }
            }
        }

        favourites.removeAll(toRemove);
        favourites.addAll(toAdd);

        mSharedPreferences.edit().putStringSet("Favourites", favourites).apply();

        if (!updatedRestaurants.isEmpty()) {
            populateUpdatedRestaurants();
        } else {
            launchMap();
        }
    }

    private void populateUpdatedRestaurants() {

        final Button okButton = findViewById(R.id.button_ok_main);
        okButton.setVisibility(View.VISIBLE);

        restaurantStrings = new String[size];
        TextView textView = findViewById(R.id.textViewMain);
        textView.setText(R.string.newly_inspected_favourite_restaurants_click_the_check_once_done);

        restaurants = updatedRestaurants;

        for (Restaurant temp : restaurants) {
            System.out.println("Test> " + temp.getName() + " Favourite: " + temp.getFavourite());
        }

        ArrayAdapter<Restaurant> adapter = new RestaurantAdapter();
        ListView restaurantList = findViewById(R.id.listViewMain);
        restaurantList.setAdapter(adapter);

        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick (View v){
            okButton.setVisibility(View.INVISIBLE);

            // Launch map as soon as we populate the list of restaurants in instance
            launchMap();
        }
        });
    }

    private void launchMap() {
        Intent i1 = new Intent(this, MapsActivity.class);
        startActivityForResult(i1, 42);

    }

    private void populateListView() throws FileNotFoundException {
        manager = RestaurantManager.getInstance();
        populateManager();


        if (manager.getRestaurants().isEmpty()) {

            TextView textView = findViewById(R.id.textViewMain);
            textView.setText(R.string.txt_select_a_restaurant);
            restaurantStrings = new String[1];
            restaurantStrings[0] = getResources().getString(R.string.greeting_main_activity);

            // Build Adapter
            ArrayAdapter<String> adapter = new ArrayAdapter<String> (
                    this,           // Context for view
                    R.layout.layout_listview,     // Layout to use
                    restaurantStrings);               // Items to be displayed

            ListView restaurantList = findViewById(R.id.listViewMain);
            restaurantList.setAdapter(adapter);

        } else {
            restaurantStrings = new String[size];
            TextView textView = findViewById(R.id.textViewMain);
            textView.setText(R.string.txt_select_a_restaurant);

            restaurants = manager.getRestaurants();
            ArrayAdapter<Restaurant> adapter = new RestaurantAdapter();
            ListView restaurantList = findViewById(R.id.listViewMain);
            restaurantList.setAdapter(adapter);
        }
    }

    private void repopulateListView() {
        manager = RestaurantManager.getInstance();


        if (manager.getRestaurants().isEmpty()) {

            restaurantStrings = new String[1];
            restaurantStrings[0] = getResources().getString(R.string.greeting_main_activity);

            // Build Adapter
            ArrayAdapter<String> adapter = new ArrayAdapter<String> (
                    this,           // Context for view
                    R.layout.layout_listview,     // Layout to use
                    restaurantStrings);               // Items to be displayed

            ListView restaurantList = findViewById(R.id.listViewMain);
            restaurantList.setAdapter(adapter);

        } else {
            restaurantStrings = new String[size];
            TextView textView = findViewById(R.id.textViewMain);
            textView.setText(R.string.txt_select_a_restaurant);

            restaurants = manager.getRestaurants();
            ArrayAdapter<Restaurant> adapter = new RestaurantAdapter();
            ListView restaurantList = findViewById(R.id.listViewMain);
            restaurantList.setAdapter(adapter);
        }
    }
    private void populateManager() throws FileNotFoundException {
        File file = method(MainActivity.this,"restaurants_itr1.csv");

        Intent i_receive = getIntent();
        String file_status = i_receive.getStringExtra(EXTRA_MESSAGE);

        InputStream is1 = null;
        if (file_status != null) {
            if (file_status.equals("OLD")) {
                is1 = getResources().openRawResource(R.raw.restaurants_itr1);
            } else {
                is1 = new FileInputStream(file);
            }
        } else {
            is1 = new FileInputStream(file);
        }

        ParseCSV csv = new ParseCSV(is1);

        // start row index at 1 to ignore the titles
        for (int row = 1; row < csv.getRowSize(); row++) {
            Restaurant restaurant = new Restaurant(
                    csv.getVal(row, 1).replace("\"", ""),
                    csv.getVal(row, 2).replace("\"", ""),
                    csv.getVal(row, 3).replace("\"", ""),
                    Float.valueOf(csv.getVal(row, 5)),
                    Float.valueOf(csv.getVal(row, 6)),
                    csv.getVal(row, 0).replace("\"", ""));

            manager.add(restaurant);

            mSharedPreferences = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE);
            Set<String> favourites = new HashSet<String>(mSharedPreferences.getStringSet("Favourites", new HashSet<String>()));

            for(String JSON: favourites) {
                Gson gson = new Gson();
                Restaurant temp = gson.fromJson(JSON, Restaurant.class);
                if(temp.getTracking().equals(restaurant.getTracking())) {
                    System.out.println("Test> " + temp.getTracking() + " vs " + restaurant.getTracking());
                    restaurant.setFavourite(true);
                }
            }
        }

        populateWithInspections();

        Collections.sort(manager.getRestaurants(), new Comparator<Restaurant>() {
            @Override
            public int compare(Restaurant o1, Restaurant o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });

    }

    private void populateWithInspections() throws FileNotFoundException {
        File file2 = method(MainActivity.this,"inspectionreports_itr1.csv");

        Intent i_receive = getIntent();
        String file_status = i_receive.getStringExtra(EXTRA_MESSAGE);
        InputStream is2 = null;
        if (file_status != null) {
            if (file_status.equals("OLD")) {
                is2 = getResources().openRawResource(R.raw.inspectionreports_itr1);
            } else {
                is2 = new FileInputStream(file2);
            }
        } else {
            is2 = new FileInputStream(file2);
        }
        ParseCSV csv2 = new ParseCSV(is2);
        String viol = "";


        for (int row = 1; row < csv2.getRowSize(); row++) {
            Inspection inspect;

            // error handling: check for valid csv file lines
            if (csv2.getVal(row, 0).equals("")) {
                break;
            }

            // multiple violations,
            // so concatenate the strings
            else if (csv2.getColSize(row) > 7) {

                for (int col = 5; col < csv2.getColSize(row) - 1; col++) {
                    viol += csv2.getVal(row, col) + " ";
                }

                inspect = new Inspection(
                        csv2.getVal(row, 0),
                        csv2.getVal(row, 1),
                        csv2.getVal(row, 2),
                        Integer.valueOf(csv2.getVal(row, 3)),
                        Integer.valueOf(csv2.getVal(row, 4)),
                        csv2.getVal(row, csv2.getColSize(row) - 1),
                        viol);


                viol = "";

                for (Restaurant restaurant : manager) {
                    if (inspect.getTrackingNumber().equals(restaurant.getTracking())) {
                        restaurant.inspections.add(inspect);
                        break;
                    }
                }
            }

            else {
                inspect = new Inspection(
                        csv2.getVal(row, 0),
                        csv2.getVal(row, 1),
                        csv2.getVal(row, 2),
                        Integer.valueOf(csv2.getVal(row, 3)),
                        Integer.valueOf(csv2.getVal(row, 4)),
                        csv2.getVal(row, 6),
                        csv2.getVal(row, 5).replace("\"", ""));


                for (Restaurant restaurant : manager) {
                    if (inspect.getTrackingNumber().equals(restaurant.getTracking())) {
                        restaurant.inspections.add(inspect);
                        break;
                    }
                }
            }


        }

        for (Restaurant restaurant : manager) {
            Collections.sort(restaurant.inspections, new Comparator<Inspection>() {
                @Override
                public int compare(Inspection o1, Inspection o2) {
                    return o2.getInspectionDate().compareTo(o1.getInspectionDate());
                }
            });
        }
    }

    static class ViewHolder {
        ImageView favourite;
    }

    private class RestaurantAdapter extends ArrayAdapter<Restaurant> {

        public RestaurantAdapter() {
            super(MainActivity.this, R.layout.restaurant_item, restaurants);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // Make sure we have a view to work with
            View itemView = convertView;

            if (itemView == null) {
                itemView = getLayoutInflater().inflate(R.layout.restaurant_item, parent, false);
            }

            // Find the restaurant to work with.
            Restaurant currentRestaurant = restaurants.get(position);

            // Fill the view
            ImageView logo = itemView.findViewById(R.id.item_restaurantLogo);
            logo.setImageResource(currentRestaurant.getIcon());

            //Favorites view
            final ImageView favourite = itemView.findViewById(R.id.item_favourite);
            favourite.setImageResource(currentRestaurant.getFavouriteImage());
            favourite.setTag(position);
            favourite.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Restaurant currentRestaurant = restaurants.get((Integer) v.getTag());
                    if (currentRestaurant.getFavourite()) {
                        currentRestaurant.setFavourite(false);
                        favourite.setImageResource(currentRestaurant.getFavouriteImage());
                        System.out.println("DD> " + currentRestaurant.getName() + "set to false\n");
                        removeFromFavourites(currentRestaurant);

                    } else if (!currentRestaurant.getFavourite()) {
                        currentRestaurant.setFavourite(true);
                        favourite.setImageResource(currentRestaurant.getFavouriteImage());
                        System.out.println("DD> " + currentRestaurant.getName() + "set to true\n");
                        saveToFavourites(currentRestaurant);
                    }
                }
            });

            TextView restaurantNameText = itemView.findViewById(R.id.item_restaurantName);
            String temp = currentRestaurant.getName();
            if (temp.length() > 30) {
                restaurantNameText.setText(temp.substring(0, 30) + "...");
            } else {
                restaurantNameText.setText(temp);
            }


            Inspection mostRecentInspection = currentRestaurant.getInspection(0);
            if (mostRecentInspection != null) {
                TextView numNonCriticalText = itemView.findViewById(R.id.item_numNonCritical);
                numNonCriticalText.setText(Integer.toString(mostRecentInspection.getNumNonCritical()));

                TextView numCriticalText = itemView.findViewById(R.id.item_numCritical);
                numCriticalText.setText(Integer.toString(mostRecentInspection.getNumCritical()));

                TextView lastInspectionText = itemView.findViewById(R.id.item_lastInspection);
                lastInspectionText.setText(mostRecentInspection.getFormattedDate());

                ImageView hazard = itemView.findViewById(R.id.item_hazard);
                hazard.setImageResource(mostRecentInspection.getHazardIcon());

            }
            return itemView;
        }

    }

    // Learned from: https://medium.com/@anupamchugh/a-nightmare-with-shared-preferences-and-stringset-c53f39f1ef52
    private void removeFromFavourites(Restaurant currentRestaurant) {
        mSharedPreferences = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE);
        Set<String> favourites = new HashSet<String>(mSharedPreferences.getStringSet("Favourites", new HashSet<String>()));
        Gson gson = new Gson();
        String json = gson.toJson(currentRestaurant);
        favourites.remove(json);
        mSharedPreferences.edit().putStringSet("Favourites", favourites).apply();
    }

    private void saveToFavourites(Restaurant currentRestaurant) {
        mSharedPreferences = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE);
        Set<String> favourites = new HashSet<String>(mSharedPreferences.getStringSet("Favourites", new HashSet<String>()));
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(currentRestaurant);
        favourites.add(json);
        editor.putStringSet("Favourites", favourites).apply();
    }

    /**
     * Calback register for RestaurantActivity
     */
    private void registerClickCallback() {
        ListView list = findViewById(R.id.listViewMain);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                if (restaurantStrings.length == 0) {
                    String message = manager.getRestaurants().get(position).toString();

                    Intent intent = RestaurantActivity.makeLaunchIntent(MainActivity.this, "RestaurantActivity");
                    intent.putExtra("Extra", message);
                    MainActivity.this.startActivityForResult(intent, 45);
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case 42:
                int answer = data.getIntExtra("result", 0);
                if (answer == 1) {
                    this.finish();
                } else {
                    repopulateListView();
                }
                break;

            case 45:
                int ans = data.getIntExtra("result", 0);
                String id = data.getStringExtra("resID");
                // 1 = launch map with peg
                // 0 = return on back
                if(ans == 1) {
                    Intent i2 = MapsActivity.makeLaunchIntent(MainActivity.this, "MapsActivity");
                    String message = id;
                    i2.putExtra("Extra", message);
                    MainActivity.this.startActivityForResult(i2, 42);
                }
                break;
            case 58:
                repopulateListView();
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case (R.id.main_map_icon):
                Intent intent = new Intent(MainActivity.this, MapsActivity.class);
                startActivityForResult(intent, 42);
                return true;
            case (R.id.main_search_icon):
                Intent i3 = new Intent(MainActivity.this, SearchActivity.class);
                startActivityForResult(i3, 58);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    static File method(Context obj, String filename){
        return new File (obj.getFilesDir(), filename );
    }
}
