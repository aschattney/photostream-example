/*
 * The MIT License
 *
 * Copyright (c) 2016 Andreas Schattney
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package de.hda.photostreamexamples.examples.plugin;


import android.content.Context;
import android.os.Bundle;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.List;

import hochschuledarmstadt.photostream_tools.IPhotoStreamClient;
import hochschuledarmstadt.photostream_tools.PhotoStreamActivity;
import hochschuledarmstadt.photostream_tools.RequestType;
import hochschuledarmstadt.photostream_tools.adapter.PluginContextualActionBar;
import hochschuledarmstadt.photostream_tools.callback.OnPhotosReceivedListener;
import de.hda.photostreamexamples.R;
import de.hda.photostreamexamples.examples.photo.PhotoAdapter;
import hochschuledarmstadt.photostream_tools.model.HttpError;
import hochschuledarmstadt.photostream_tools.model.Photo;
import hochschuledarmstadt.photostream_tools.model.PhotoQueryResult;

public class ContextualActionBarPluginActivity extends PhotoStreamActivity implements OnPhotosReceivedListener {

    private static final String KEY_ADAPTER = "KEY_ADAPTER";
    private static final String KEY_BUTTON_VISIBILITY = "KEY_BUTTON_VISIBILITY";

    private PhotoAdapter adapter;
    private RecyclerView recyclerView;
    private Button button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plugin_example);

        // "Mehr Laden" - Button referenzieren
        button = (Button) findViewById(R.id.button);

        // Wenn auf den Button gedrückt wurde,
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // und nicht bereits ein Request für Photos läuft,
                if(!getPhotoStreamClient().hasOpenRequestOfType(RequestType.LOAD_PHOTOS)) {
                    // dann zunächst den Button deaktivieren
                    button.setEnabled(false);
                    // und anschließend die nächste Seite von Photos aus dem Stream laden
                    getPhotoStreamClient().loadMorePhotos();
                }
            }
        });

        if (savedInstanceState != null)
            button.setVisibility(savedInstanceState.getBoolean(KEY_BUTTON_VISIBILITY) ? Button.VISIBLE : Button.GONE);

        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new PhotoAdapter();

        adapter.addOnLongClickPlugin(R.id.itemRoot, new PluginContextualActionBar<Photo, PhotoAdapter.PhotoViewHolder>(this, R.menu.menu_cab_example) {
            @Override
            protected boolean onActionItemClicked(ActionMode mode, MenuItem menuItem, List<Integer> selectedItemIds) {
                if (menuItem.getItemId() == R.id.action_delete){
                    Toast.makeText(ContextualActionBarPluginActivity.this, getString(R.string.delete), Toast.LENGTH_SHORT).show();
                    return true;
                }
                if (menuItem.getItemId() == R.id.action_share){
                    Toast.makeText(ContextualActionBarPluginActivity.this, getString(R.string.share), Toast.LENGTH_SHORT).show();
                    return true;
                }
                return false;
            }

            @Override
            protected void onUpdateContextualActionBar(ActionMode actionMode, Context context, List<Photo> photos, int selectedItemsCount) {
                Photo photo = photos.get(0);
                actionMode.setTitle(photo.getDescription());
                actionMode.getMenu().findItem(R.id.action_delete).setVisible(photo.isDeleteable());
            }

            @Override
            protected void onActionModeFinished() {

            }

            @Override
            protected boolean shouldExecute(PhotoAdapter.PhotoViewHolder viewHolder, View v, Photo item) {
                return getAmountSelectedItems() == 0;  // <-- es darf immer nur ein Photo markiert sein
                // return item.isDeleteable()           <-- nur Photos, die löschbar sind, können markiert werden
                // return true; //                      <-- alle Photos können markiert werden
            }
        });
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onPhotoStreamServiceConnected(IPhotoStreamClient photoStreamClient, Bundle savedInstanceState) {
        photoStreamClient.addOnPhotosReceivedListener(this);
        if (savedInstanceState == null)
            photoStreamClient.loadPhotos();
    }

    @Override
    protected void onPhotoStreamServiceDisconnected(IPhotoStreamClient photoStreamClient) {
        photoStreamClient.removeOnPhotosReceivedListener(this);
    }

    @Override
    public void onPhotosReceived(PhotoQueryResult result) {
        button.setEnabled(true);
        if (result.isFirstPage())
            adapter.set(result.getPhotos());
        else
            adapter.addAll(result.getPhotos());

        button.setVisibility(result.hasNextPage() ? Button.VISIBLE : Button.GONE);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBundle(KEY_ADAPTER, adapter.saveInstanceState());
        outState.putBoolean(KEY_BUTTON_VISIBILITY, button.getVisibility() == Button.VISIBLE);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        adapter.restoreInstanceState(savedInstanceState.getBundle(KEY_ADAPTER));
    }

    @Override
    public void onReceivePhotosFailed(HttpError httpError) {
        button.setEnabled(true);
    }

    @Override
    public void onNoNewPhotosAvailable() {

    }

}
