/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2014, Enno Gottschalk <mrmaffen@googlemail.com>
 *
 *   Tomahawk is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Tomahawk is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with Tomahawk. If not, see <http://www.gnu.org/licenses/>.
 */
package org.tomahawk.tomahawk_android.fragments;

import org.tomahawk.libtomahawk.collection.UserPlaylist;
import org.tomahawk.libtomahawk.database.UserPlaylistsDataSource;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.utils.TomahawkUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.adapters.TomahawkListAdapter;
import org.tomahawk.tomahawk_android.services.PlaybackService;
import org.tomahawk.tomahawk_android.utils.TomahawkListItem;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link org.tomahawk.tomahawk_android.fragments.TomahawkFragment} which shows information provided
 * by a User object. Such as the image, feed and nowPlaying info of a user.
 */
public class UserFragment extends TomahawkFragment implements OnItemClickListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();

        updateAdapter();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (mAlbum != null) {
            menu.findItem(R.id.action_gotoartist_item).setVisible(true);
        }

        super.onCreateOptionsMenu(menu, inflater);
    }

    /**
     * If the user clicks on a menuItem, handle what should be done here
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item != null) {
            if (item.getItemId() == R.id.action_gotoartist_item) {
                Bundle bundle = new Bundle();
                String key = TomahawkUtils.getCacheKey(mAlbum.getArtist());
                bundle.putString(TOMAHAWK_ARTIST_KEY, key);
                mTomahawkApp.getContentViewer()
                        .replace(AlbumsFragment.class, key, TOMAHAWK_ARTIST_KEY, false, false);
            }
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Called every time an item inside the {@link se.emilsjolander.stickylistheaders.StickyListHeadersListView}
     * is clicked
     *
     * @param parent   The AdapterView where the click happened.
     * @param view     The view within the AdapterView that was clicked (this will be a view
     *                 provided by the adapter)
     * @param position The position of the view in the adapter.
     * @param id       The row id of the item that was clicked.
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        position -= getListView().getHeaderViewsCount();
        if (position >= 0) {
            if (getListAdapter().getItem(position) instanceof Query
                    && ((Query) getListAdapter().getItem(position)).isPlayable()) {
                ArrayList<Query> queries = new ArrayList<Query>();
                if (mAlbum != null) {
                    queries = mAlbum.getQueries(mIsLocal);
                } else if (mArtist != null) {
                    queries = mArtist.getQueries(mIsLocal);
                } else if (mUserPlaylist != null) {
                    queries = mUserPlaylist.getQueries();
                } else {
                    queries.addAll(mTomahawkMainActivity.getUserCollection().getQueries());
                }
                PlaybackService playbackService = mTomahawkMainActivity.getPlaybackService();
                if (playbackService != null && shouldShowPlaystate()
                        && playbackService.getCurrentPlaylist().getCurrentQueryIndex()
                        == mShownAlbums.size() + mShownArtists.size() + position) {
                    playbackService.playPause();
                } else {
                    UserPlaylist playlist = UserPlaylist
                            .fromQueryList(UserPlaylistsDataSource.CACHED_PLAYLIST_ID,
                                    UserPlaylistsDataSource.CACHED_PLAYLIST_NAME, queries,
                                    queries.get(position));
                    if (playbackService != null) {
                        playbackService.setCurrentPlaylist(playlist);
                        playbackService.start();
                    }
                }
            }
        }
    }

    /**
     * Update this {@link org.tomahawk.tomahawk_android.fragments.TomahawkFragment}'s {@link
     * org.tomahawk.tomahawk_android.adapters.TomahawkBaseAdapter} content
     */
    public void updateAdapter() {
        ArrayList<TomahawkListItem> socialActions
                = new ArrayList<TomahawkListItem>(mUser.getSocialActions());
        TomahawkListAdapter tomahawkListAdapter;
        if (mUser != null) {
            mTomahawkMainActivity.setTitle(mUser.getName());
            List<List<TomahawkListItem>> listArray
                    = new ArrayList<List<TomahawkListItem>>();
            listArray.add(socialActions);
            if (getListAdapter() == null) {
                tomahawkListAdapter = new TomahawkListAdapter(mTomahawkMainActivity, listArray);
                tomahawkListAdapter.setShowResolvedBy(true);
                tomahawkListAdapter.setShowCategoryHeaders(true, false);
                tomahawkListAdapter.showContentHeader(getListView(), mUser, mIsLocal);
                setListAdapter(tomahawkListAdapter);
            } else {
                ((TomahawkListAdapter) getListAdapter()).setListArray(listArray);
                ((TomahawkListAdapter) getListAdapter()).updateContentHeader(mUser, mIsLocal);
            }
        }

        /*mShownQueries.clear();
        for (TomahawkListItem item : socialActions) {
            mShownQueries.add((Query) item);
        }*/

        getListView().setOnItemClickListener(this);
    }

    @Override
    protected void onPipeLineResultsReported(ArrayList<String> queryKeys) {
        for (String key : queryKeys) {
            if (mCorrespondingQueryIds.contains(key)) {
                updateAdapter();
                break;
            }
        }
    }

    @Override
    protected void onInfoSystemResultsReported(String requestId) {
        if (mCurrentRequestIds.contains(requestId)) {
            updateAdapter();
            resolveVisibleQueries();
        }
    }
}