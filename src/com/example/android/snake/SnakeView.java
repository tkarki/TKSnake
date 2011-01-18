/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.snake;

import java.util.ArrayList;
import java.util.Random;

import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

/**
 * SnakeView: implementation of a simple game of Snake
 * 
 * v1.0 some improvements - snake speed mostly. [TK]
 * v1.1 added stones that kills you. [TK]
 * v1.2 improved stones - not getting into same coordinates as Apples. [TK]
 * v1.3 More stones and apples.
 * v1.4 Buggfixar.
 * v1.45 Highscore lista.
 * 
 * PLANNED:
 * v2.0 now with levels and highscore list.
 */

public class SnakeView extends TileView {

    private static final String TAG = "SnakeView";

    /**
     * Current mode of application: READY to run, RUNNING, or you have already
     * lost. static final ints are used instead of an enum for performance
     * reasons.
     */
    private int mMode = READY;
    public static final int PAUSE = 0;
    public static final int READY = 1;
    public static final int RUNNING = 2;
    public static final int LOSE = 3;

    /**
     * Current direction the snake is headed.
     */
    private int mDirection = NORTH;
    private int mNextDirection = NORTH;
    private static final int NORTH = 1;
    private static final int SOUTH = 2;
    private static final int EAST = 3;
    private static final int WEST = 4;

    /**
     * Labels for the drawables that will be loaded into the TileView class
     */
    private static final int RED_STAR = 1;
    private static final int YELLOW_STAR = 2;
    private static final int GREEN_STAR = 3;
    private static final int GREY_STAR = 4;

    /**
     * mScore: used to track the number of apples captured mMoveDelay: number of
     * milliseconds between snake movements. This will decrease as apples are
     * captured.
     */
    private long mScore = 0;
    private long mMoveDelay;
    private double mMoveDelaySub;
    private double mMoveDelaySubKonst = 0.994;
    
    private long max_delay = 450; //startvärde
    private long min_delay = 100; // se det som ett riktmärke. når hit när speed är oändligt stor
    private long retardation = 15; // ska vara mindre än noll.  högt värde = snabb acceleration
    private long max_speed = 150; //utan gräns kommer det ta sjukt lång tid att bromsa
    private long delay = max_delay;
    private long speed = max_speed;
    private long retcount = 0;

    private int startx=0, starty=0, stopx=0, stopy=0;
    
    /**
     * mLastMove: tracks the absolute time when the snake last moved, and is used
     * to determine if a move should be made based on mMoveDelay.
     */
    private long mLastMove;
    
    /**
     * mStatusText: text shows to the user in some run states
     */
    private TextView mStatusText;
    private TextView mTitleText;

    /**
     * mSnakeTrail: a list of Coordinates that make up the snake's body
     * mAppleList: the secret location of the juicy apples the snake craves.
     */
    private ArrayList<Coordinate> mSnakeTrail = new ArrayList<Coordinate>();
    private ArrayList<Coordinate> mAppleList = new ArrayList<Coordinate>();
    private ArrayList<Coordinate> mStoneList = new ArrayList<Coordinate>();

    /**
     * Everyone needs a little randomness in their life
     */
    private static final Random RNG = new Random();

    /**
     * Create a simple handler that we can use to cause animation to happen.  We
     * set ourselves as a target and we can use the sleep()
     * function to cause an update/invalidate to occur at a later date.
     */
    private RefreshHandler mRedrawHandler = new RefreshHandler();
    class RefreshHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            SnakeView.this.update();
            SnakeView.this.invalidate();
        }

        public void sleep(long delayMillis) {
        	this.removeMessages(0);
            sendMessageDelayed(obtainMessage(0), delayMillis);
        }
    };

    Highscore highscore;
    
    /**
     * Constructs a SnakeView based on inflation from XML
     * 
     * @param context
     * @param attrs
     */
    public SnakeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        highscore = new Highscore(context);
        initSnakeView();
   }

    public SnakeView(Context context, AttributeSet attrs, int defStyle) {
    	super(context, attrs, defStyle);
        highscore = new Highscore(context);
    	initSnakeView();
    }

    private void initSnakeView() {
        setFocusable(true);

        Resources r = this.getContext().getResources();
        
        resetTiles(8);
        loadTile(RED_STAR, r.getDrawable(R.drawable.redstar));
        loadTile(YELLOW_STAR, r.getDrawable(R.drawable.yellowstar));
        loadTile(GREEN_STAR, r.getDrawable(R.drawable.greenstar));
        loadTile(GREY_STAR, r.getDrawable(R.drawable.greystar));

    }
    

    private void initNewGame() {
        mSnakeTrail.clear();
        mAppleList.clear();
        mStoneList.clear();

        // For now we're just going to load up a short default eastbound snake
        // that's just turned north

        int size = (mXTileCount * mYTileCount) / 64;
        for(int index=size;index > 4;index--) {
            mSnakeTrail.add(new Coordinate(3, index));
        }
        for(int index=3;index < size-4;index++) {
            mSnakeTrail.add(new Coordinate(index, 4));        	
        }
        mNextDirection = SOUTH;

        int applecount = (mXTileCount * mYTileCount) / 54;
        int stonecount = (mXTileCount * mYTileCount) / 24;

        // Add some Stones to start with.
        for (int index=0; index < stonecount;index++) {
        	addRandomStone();
        }
        
        // And some apples to start with
        for (int index=0; index < applecount;index++) {
        	addRandomApple();
        }
        
        mMoveDelay = 400;
        mMoveDelaySub = 0.9;
        mScore = 0;

        delay = max_delay;
        speed = max_speed;
        retardation = 15;
        retcount = 0;
     
        UpdateTitleText();
    }


    /**
     * Given a ArrayList of coordinates, we need to flatten them into an array of
     * ints before we can stuff them into a map for flattening and storage.
     * 
     * @param cvec : a ArrayList of Coordinate objects
     * @return : a simple array containing the x/y values of the coordinates
     * as [x1,y1,x2,y2,x3,y3...]
     */
    private int[] coordArrayListToArray(ArrayList<Coordinate> cvec) {
        int count = cvec.size();
        int[] rawArray = new int[count * 2];
        for (int index = 0; index < count; index++) {
            Coordinate c = cvec.get(index);
            rawArray[2 * index] = c.x;
            rawArray[2 * index + 1] = c.y;
        }
        return rawArray;
    }

    /**
     * Save game state so that the user does not lose anything
     * if the game process is killed while we are in the 
     * background.
     * 
     * @return a Bundle with this view's state
     */
    public Bundle saveState() {
        Bundle map = new Bundle();

        map.putIntArray("mAppleList", coordArrayListToArray(mAppleList));
        map.putIntArray("mStoneList", coordArrayListToArray(mAppleList));
        map.putInt("mDirection", Integer.valueOf(mDirection));
        map.putInt("mNextDirection", Integer.valueOf(mNextDirection));
        map.putLong("mMoveDelay", Long.valueOf(mMoveDelay));
        map.putLong("mScore", Long.valueOf(mScore));
        map.putIntArray("mSnakeTrail", coordArrayListToArray(mSnakeTrail));

        return map;
    }

    /**
     * Given a flattened array of ordinate pairs, we reconstitute them into a
     * ArrayList of Coordinate objects
     * 
     * @param rawArray : [x1,y1,x2,y2,...]
     * @return a ArrayList of Coordinates
     */
    private ArrayList<Coordinate> coordArrayToArrayList(int[] rawArray) {
        ArrayList<Coordinate> coordArrayList = new ArrayList<Coordinate>();

        int coordCount = rawArray.length;
        for (int index = 0; index < coordCount; index += 2) {
            Coordinate c = new Coordinate(rawArray[index], rawArray[index + 1]);
            coordArrayList.add(c);
        }
        return coordArrayList;
    }

    /**
     * Restore game state if our process is being relaunched
     * 
     * @param icicle a Bundle containing the game state
     */
    public void restoreState(Bundle icicle) {
        setMode(PAUSE);

        mAppleList = coordArrayToArrayList(icicle.getIntArray("mAppleList"));
        mStoneList = coordArrayToArrayList(icicle.getIntArray("mStoneList"));
        mDirection = icicle.getInt("mDirection");
        mNextDirection = icicle.getInt("mNextDirection");
        mMoveDelay = icicle.getLong("mMoveDelay");
        mScore = icicle.getLong("mScore");
        mSnakeTrail = coordArrayToArrayList(icicle.getIntArray("mSnakeTrail"));
    }

    /* Returnerar true om skillnaden mellan startx och stopx är större än skillnaden mellan starty och stopy. */
    
    private boolean diffxy(int startx, int starty, int stopx, int stopy) {
    	int xdiff = startx - stopx;
    	int ydiff = starty - stopy;
    	int xdiff2 = xdiff;
    	int ydiff2 = ydiff;
    	
    	if (xdiff2 < 0) {
    		xdiff2 = -xdiff2;
    	}
    	if (ydiff2 < 0) { 
    		ydiff2 = -ydiff2; 
    	}
    	
    	if (xdiff2 > ydiff2) {
    		return true;
    	} else {
    		return false;
    	}
    	
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
    	switch (event.getAction()) {

        // get your x and y values from event.getX() and event.getY()
      case MotionEvent.ACTION_DOWN:
              startx = (int) event.getX();
              starty = (int) event.getY();
      break;

      case MotionEvent.ACTION_UP:
  		if (mMode == READY | mMode == LOSE) {
			/*
			 * At the beginning of the game, or the end of a previous one,
			 * we should start a new game.
			 */
			initNewGame();
			setMode(RUNNING);
			update();
			return true;
		}

          stopx = (int) event.getX();
          stopy = (int) event.getY();
          if (diffxy(startx, starty, stopx, stopy)) {
        	if (startx-stopx > 0) {
        		if (mDirection != EAST) { mNextDirection = WEST; }
        	} else {
        		if (mDirection != WEST) { mNextDirection = EAST; }
        	}
          } else {
        	if (starty-stopy > 0) {
        		if (mDirection != SOUTH) { mNextDirection = NORTH; }
        	} else {
        		if (mDirection != NORTH) { mNextDirection = SOUTH; }        		
        	}
          }
          
          
          UpdateTitleText();
      break;

      }

      return true;
    }
    
    
     
    /*
     * handles key events in the game. Update the direction our snake is traveling
     * based on the DPAD. Ignore events that would cause the snake to immediately
     * turn back on itself.
     * 
     * (non-Javadoc)
     * 
     * @see android.view.View#onKeyDown(int, android.os.KeyEvent)
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent msg) {

    	if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER | keyCode == KeyEvent.KEYCODE_SPACE) {
    		if (mMode == READY | mMode == LOSE) {
    			/*
    			 * At the beginning of the game, or the end of a previous one,
    			 * we should start a new game.
    			 */
    			initNewGame();
    			setMode(RUNNING);
    			update();
    			return (true);
    		}

    		if (mMode == PAUSE) {
    			/*
    			 * If the game is merely paused, we should just continue where
    			 * we left off.
    			 */
    			setMode(RUNNING);
    			update();
    			return (true);
    		}
    	}

    	// Sväng vänster
    	if (keyCode == KeyEvent.KEYCODE_BACK) {
    		if (mMode == READY | mMode == LOSE) {
    			
    		} else {
    			switch (mDirection) {
    			case SOUTH:
    				mNextDirection = WEST;
    				return (true);
    			case NORTH:
    				mNextDirection = EAST;
    				return (true);
    			case WEST:
    				mNextDirection = NORTH;
    				return (true);
    			case EAST:
    				mNextDirection = SOUTH;
    				return (true);
    			}
    		}
        }
    	
    	// Sväng höger
    	if (keyCode == KeyEvent.KEYCODE_MENU) {
    		switch (mDirection) {
    		case SOUTH:
    			mNextDirection = EAST;
    			return (true);
    		case NORTH:
    			mNextDirection = WEST;
    			return (true);
    		case WEST:
    			mNextDirection = SOUTH;
    			return (true);
    		case EAST:
    			mNextDirection = NORTH;
    			return (true);
    		}
        }
    	
    	if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            if (mDirection != SOUTH) {
                mNextDirection = NORTH;
            }
            return (true);
        }

        if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            if (mDirection != NORTH) {
                mNextDirection = SOUTH;
            }
            return (true);
        }

        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            if (mDirection != EAST) {
                mNextDirection = WEST;
            }
            return (true);
        }

        if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            if (mDirection != WEST) {
                mNextDirection = EAST;
            }
            return (true);
        }

        return super.onKeyDown(keyCode, msg);
    }

    /**
     * Sets the TextView that will be used to give information (such as "Game
     * Over" to the user.
     * 
     * @param newView
     */
    public void setTextView(TextView newView) {
        mStatusText = newView;
    }

    public void setTitleTextView(TextView newView) {
        mTitleText = newView;
    }

    /**
     * Updates the current mode of the application (RUNNING or PAUSED or the like)
     * as well as sets the visibility of textview for notification
     * 
     * @param newMode
     */
    public void setMode(int newMode) {
        int oldMode = mMode;
        mMode = newMode;

        if (newMode == RUNNING & oldMode != RUNNING) {
            mStatusText.setVisibility(View.INVISIBLE);
            update();
            UpdateTitleText();
            return;
        }

        Resources res = getContext().getResources();
        CharSequence str = "";
        if (newMode == PAUSE) {
            str = res.getText(R.string.Application_Name)+" "+res.getText(R.string.Version)+" "+res.getText(R.string.mode_pause)
            + "   " + mXTileCount + " " + mYTileCount + " " + mXTileCount * mYTileCount;
        }
        if (newMode == READY) {
            str = res.getText(R.string.Application_Name)+" "+res.getText(R.string.Version)+" "+res.getText(R.string.mode_ready);
        }
        if (newMode == LOSE) {
        	if (highscore.inHighscore(mScore)) {
        		int pos;
        		pos = highscore.addScore("TK", mScore) + 1;
        		str = res.getString(R.string.Application_Name) + " " + res.getString(R.string.Version) + "\n"
            	+ res.getString(R.string.mode_lose_prefix) + mScore + "\n"
            	+ "HIGHSCORE pos: " + pos + ". " + mScore + "p";
        	} else {
        		str = res.getString(R.string.Application_Name) + " " + res.getString(R.string.Version) + "\n"
            	+ res.getString(R.string.mode_lose_prefix) + mScore
            	+ res.getString(R.string.mode_lose_suffix);
        	}
        }

        mStatusText.setText(str);
        mStatusText.setVisibility(View.VISIBLE);
    }

    public void UpdateTitleText() {
        Resources res = getContext().getResources();
        CharSequence str = "";
        int stonecount = mStoneList.size();
        // str = "Snake "+res.getString(R.string.Version)+" Score "+mScore+" MoveDelay "+mMoveDelay+" MoveDelaySub "+mMoveDelaySub;
        str = "Snake "+res.getString(R.string.Version)+" Score: "+mScore+ " Stones: "+stonecount+" "+delay;
        // str = "Score: "+mScore+" "+startx+" "+starty+" "+stopx+" "+stopy;
        mTitleText.setText(str);
        mTitleText.setVisibility(View.VISIBLE);

    }
    
    /**
     * Selects a random location within the garden that is not currently covered
     * by the snake. Currently _could_ go into an infinite loop if the snake
     * currently fills the garden, but we'll leave discovery of this prize to a
     * truly excellent snake-player.
     * 
     */
    private void addRandomApple() {
        Coordinate newCoord = null;
        boolean found = false;
        while (!found) {
            // Choose a new location for our apple
            int newX = 1 + RNG.nextInt(mXTileCount - 2);
            int newY = 2 + RNG.nextInt(mYTileCount - 3);
            newCoord = new Coordinate(newX, newY);

            // Make sure it's not already under the snake
            boolean collision = false;
            int snakelength = mSnakeTrail.size();
            for (int index = 0; index < snakelength; index++) {
                if (mSnakeTrail.get(index).equals(newCoord)) {
                    collision = true;
                }
            }

            // If no collision with Snake, also make sure it won't conflict with stones.
            if (!collision) {
                int stonelength = mStoneList.size();
                for (int index = 0; index < stonelength; index++) {
                    if (mStoneList.get(index).equals(newCoord)) {
                        collision = true;
                    }
                }           	
            }
            
            // if we're here and there's been no collision, then we have
            // a good location for an apple. Otherwise, we'll circle back
            // and try again
            found = !collision;
        }
        if (newCoord == null) {
            Log.e(TAG, "Somehow ended up with a null newCoord!");
        }
        mAppleList.add(newCoord);
    }

    /**
     * Selects a random location within the garden that is not currently covered
     * by the snake. 
     * 
     */
    private void addRandomStone() {
        Coordinate newCoord = null;
        boolean found = false;
        while (!found) {
            // Choose a new location for our apple
            int newX = 1 + RNG.nextInt(mXTileCount - 2);
            int newY = 2 + RNG.nextInt(mYTileCount - 3);
            newCoord = new Coordinate(newX, newY);

            // Make sure it's not already under the snake
            boolean collision = false;
            int snakelength = mSnakeTrail.size();
            for (int index = 0; index < snakelength; index++) {
                if (mSnakeTrail.get(index).equals(newCoord)) {
                    collision = true;
                }
            }
            
            // If no collision with Snake, also make sure it won't conflict with apples.
            if (!collision) {
                int applelength = mAppleList.size();
                for (int index = 0; index < applelength; index++) {
                    if (mAppleList.get(index).equals(newCoord)) {
                        collision = true;
                    }
                }           	
            }

            // if we're here and there's been no collision, then we have
            // a good location for an apple. Otherwise, we'll circle back
            // and try again
            found = !collision;
        }
        if (newCoord == null) {
            Log.e(TAG, "Somehow ended up with a null newCoord!");
        }
        mStoneList.add(newCoord);
    }


    
    /**
     * Handles the basic update loop, checking to see if we are in the running
     * state, determining if a move should be made, updating the snake's location.
     */
    public void update() {
        if (mMode == RUNNING) {
            long now = System.currentTimeMillis();

            if (now - mLastMove > delay) {
                clearTiles();
                updateWalls();
                updateSnake();
                updateApples();
                updateStones();
                mLastMove = now;
            }
            mRedrawHandler.sleep(delay);
        }

    }

    /**
     * Draws some walls.
     * 
     */
    private void updateWalls() {
        for (int x = 0; x < mXTileCount; x++) {
            setTile(GREEN_STAR, x, 1);
            setTile(GREEN_STAR, x, mYTileCount - 1);
        }
        for (int y = 1; y < mYTileCount - 1; y++) {
            setTile(GREEN_STAR, 0, y);
            setTile(GREEN_STAR, mXTileCount - 1, y);
        }
    }

    /**
     * Draws some apples.
     * 
     */
    private void updateApples() {
        for (Coordinate c : mAppleList) {
            setTile(YELLOW_STAR, c.x, c.y);
        }
    }

    /**
     * Draws some stones.
     * 
     */
    private void updateStones() {
        for (Coordinate c : mStoneList) {
            setTile(GREY_STAR, c.x, c.y);
        }
    }

    /**
     * Figure out which way the snake is going, see if he's run into anything (the
     * walls, himself, or an apple). If he's not going to die, we then add to the
     * front and subtract from the rear in order to simulate motion. If we want to
     * grow him, we don't subtract from the rear.
     * 
     */
    private void updateSnake() {
        boolean growSnake = false;

        // grab the snake by the head
        Coordinate head = mSnakeTrail.get(0);
        Coordinate newHead = new Coordinate(1, 1);

        mDirection = mNextDirection;

        switch (mDirection) {
        case EAST: {
            newHead = new Coordinate(head.x + 1, head.y);
            break;
        }
        case WEST: {
            newHead = new Coordinate(head.x - 1, head.y);
            break;
        }
        case NORTH: {
            newHead = new Coordinate(head.x, head.y - 1);
            break;
        }
        case SOUTH: {
            newHead = new Coordinate(head.x, head.y + 1);
            break;
        }
        }

        // Collision detection
        // For now we have a 1-square wall around the entire arena
        if ((newHead.x < 1) || (newHead.y < 2) || (newHead.x > mXTileCount - 2)
                || (newHead.y > mYTileCount - 2)) {
            setMode(LOSE);
            return;

        }

        // Look for collisions with itself
        int snakelength = mSnakeTrail.size();
        for (int snakeindex = 0; snakeindex < snakelength; snakeindex++) {
            Coordinate c = mSnakeTrail.get(snakeindex);
            if (c.equals(newHead)) {
                setMode(LOSE);
                return;
            }
        }

        // Look for collisions with stones
        int stonecount = mStoneList.size();
        for (int stoneindex = 0; stoneindex < stonecount; stoneindex++) {
            Coordinate c = mStoneList.get(stoneindex);
            if (c.equals(newHead)) {
                setMode(LOSE);
                return;
            }
        }

        // Look for apples
        int applecount = mAppleList.size();
        for (int appleindex = 0; appleindex < applecount; appleindex++) {
            Coordinate c = mAppleList.get(appleindex);
            if (c.equals(newHead)) {
                mAppleList.remove(c);
                addRandomApple();
                addRandomStone();
                addRandomStone();
                
                mScore++;
                mMoveDelay *= mMoveDelaySub;
                mMoveDelaySub = mMoveDelaySub / mMoveDelaySubKonst;
                decelerate();
                
                UpdateTitleText();
                growSnake = true;
            }
        }

        // push a new head onto the ArrayList and pull off the tail
        mSnakeTrail.add(0, newHead);
        // except if we want the snake to grow
        if (!growSnake) {
            mSnakeTrail.remove(mSnakeTrail.size() - 1);
        }

        int index = 0;
        for (Coordinate c : mSnakeTrail) {
            if (index == 0) {
                setTile(YELLOW_STAR, c.x, c.y);
            } else {
                setTile(RED_STAR, c.x, c.y);
            }
            index++;
        }

    }

    /**
     * Simple class containing two integer values and a comparison function.
     * There's probably something I should use instead, but this was quick and
     * easy to build.
     * 
     */
    private class Coordinate {
        public int x;
        public int y;

        public Coordinate(int newX, int newY) {
            x = newX;
            y = newY;
        }

        public boolean equals(Coordinate other) {
            if (x == other.x && y == other.y) {
                return true;
            }
            return false;
        }

        @Override
        public String toString() {
            return "Coordinate: [" + x + "," + y + "]";
        }
    }
    
    

    /*
    public void accelerate(){
    	if(speed < max_speed)
    	  speed++;
    	  delay = min_delay + (int) (max_delay /1+retardation*speed);
    	}

    public void decelerate(){
    if(speed > 0)
      speed = speed - 10; //vill kanske bromsa snabbare än vi accelererar
      if(speed < 0)
        speed = 0;
      delay = min_delay + (int) (max_delay /1+retardation*speed);
    }
    */
    
    public void decelerate(){
    	delay = delay - retardation;
    	if(delay < min_delay) {
    		delay = min_delay;
    	}
    	retcount++;
    	if (retcount == 3) {
        	retardation = retardation - 1;
        	retcount = 0;
    	}
    	if (retardation < 0) {
    		retardation = 0;
    	}
    	
    }
    
} // end of Snakeview


