package com.codepath.apps.restclienttemplate

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.codepath.apps.restclienttemplate.models.Tweet
import com.codepath.asynchttpclient.callback.JsonHttpResponseHandler
import okhttp3.Headers
import org.json.JSONException
import com.codepath.apps.restclienttemplate.models.EndlessRecyclerViewScrollListener






class TimelineActivity : AppCompatActivity() {
    lateinit var swipeContainer: SwipeRefreshLayout

    lateinit var scrollListener: EndlessRecyclerViewScrollListener


    lateinit var client: TwitterClient
    lateinit var rvTweets: RecyclerView
    lateinit var adapter: TweetsAdapter
    val tweets = ArrayList<Tweet>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_timeline)

        val linearLayoutManager = LinearLayoutManager(this)
        client = TwitterApplication.getRestClient(this)

        swipeContainer = findViewById(R.id.swipeContainer)

        swipeContainer.setOnRefreshListener {
            Log.i(TAG, "Refreshing timeline")
            populateHomeTimeline()
        }

        swipeContainer.setColorSchemeResources(android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_red_light
        )

        rvTweets = findViewById(R.id.rvTweets)
        adapter = TweetsAdapter(tweets)

        rvTweets.layoutManager = LinearLayoutManager(this)
        rvTweets.adapter = adapter
        scrollListener = object : EndlessRecyclerViewScrollListener(linearLayoutManager) {
            override fun onLoadMore(page: Int, totalItemsCount: Int, view: RecyclerView?) {
                // Triggered only when new data needs to be appended to the list
                // Add whatever code is needed to append new items to the bottom of the list
                loadMoreData()
            }
        }

        rvTweets.addOnScrollListener(scrollListener)

        populateHomeTimeline()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if(item?.itemId == R.id.compose){
            val intent = Intent(this, ComposeActivity::class.java)
            publishTweetActivityResultLauncher.launch(intent)
        }

        return super.onOptionsItemSelected(item)
    }

    var publishTweetActivityResultLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // If the user comes back to this activity from EditActivity
        // with no error or cancellation
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            // Get the data passed from EditActivity
            if (data != null) {
                val tweet = data.extras!!.getParcelable("tweet") as Tweet

                tweets.add(0, tweet)
                adapter.notifyItemInserted(0)
                rvTweets.scrollToPosition(0)
            }


        }
    }

    fun loadMoreData(){
        client.getNextPageOfTweets(object : JsonHttpResponseHandler(){

            override fun onSuccess(statusCode: Int, headers: Headers, json: JSON) {
                Log.i(TAG, "onSuccess for load more data!")

                val jsonArray = json.jsonArray

                try{
                    val listsOfNewTweetsRetrieved = Tweet.fromJsonArray(jsonArray)
                    adapter.addAll(listsOfNewTweetsRetrieved)
                    adapter.notifyDataSetChanged()
                    swipeContainer.setRefreshing(false)
                } catch (e: JSONException){
                    Log.e(TAG, "JSON Exception $e")
                }
            }

            override fun onFailure(
                statusCode: Int,
                headers: Headers?,
                response: String?,
                throwable: Throwable?
            ) {
                Log.e(TAG, "onFailure $statusCode $response")
            }


        }, tweets.get(tweets.size - 1).id)
    }

    fun populateHomeTimeline(){
        client.getHomeTimeline(object : JsonHttpResponseHandler(){

            override fun onSuccess(statusCode: Int, headers: Headers, json: JSON) {
                Log.i(TAG, "onSuccess!")

                val jsonArray = json.jsonArray

                try{
                    adapter.clear()
                    val listsOfNewTweetsRetrieved = Tweet.fromJsonArray(jsonArray)
                    tweets.addAll(listsOfNewTweetsRetrieved)
                    adapter.notifyDataSetChanged()
                    swipeContainer.setRefreshing(false)
                } catch (e: JSONException){
                    Log.e(TAG, "JSON Exception $e")
                }
            }

            override fun onFailure(
                statusCode: Int,
                headers: Headers?,
                response: String?,
                throwable: Throwable?
            ) {
                Log.e(TAG, "onFailure $statusCode $response")
            }


        })
    }


    companion object{
        val TAG = "TimelineActivity"
        val REQUEST_CODE = 20
    }
}