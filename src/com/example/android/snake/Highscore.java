package com.example.android.snake;

import android.content.Context;
import android.content.SharedPreferences;

/*
If you want to test, if the score is in the Highscore-List, you can do
	it like this:
	highscore.inHighscore(score);
	It is helpful, if you want to ask for the name just if the person is
	in the Highscore-List.

	You get the name of the x-th position in the Highscore like this:
	highscore.getName(x);

	The same for the score:
	getScore(x);
*/


//	Here is the Highscore-Class:


public class Highscore {
	private SharedPreferences preferences;
	private String names[];
	private long score[];

	public Highscore(Context context)
	{
	preferences = context.getSharedPreferences("Highscore", 0);
	names = new String[10];
	score = new long[10];

	for (int x=0; x<10; x++)
	{
	names[x] = preferences.getString("name"+x, "-");
	score[x] = preferences.getLong("score"+x, 0);
	}

	}

	public String getName(int x)
	{
	//get the name of the x-th position in the Highscore-List
	return names[x];
	}

	public long getScore(int x)
	{
	//get the score of the x-th position in the Highscore-List
	return score[x];
	}

	public boolean inHighscore(long score)
	{
	//test, if the score is in the Highscore-List
	int position;
	for (position=0; position<10&&this.score[position]>score; 
	position++);

	if (position==10) return false;
	return true;
	}

	public int addScore(String name, long score)
	{
	//add the score with the name to the Highscore-List
	int position;
	for (position=0; position<10&&this.score[position]>score; 
	position++);

	if (position==10) return -1;

	for (int x=9; x>position; x--)
	{
	names[x]=names[x-1];
	this.score[x]=this.score[x-1];
	}

	this.names[position] = new String(name);
	this.score[position] = score;

	SharedPreferences.Editor editor = preferences.edit();
	for (int x=0; x<10; x++)
	{
	editor.putString("name"+x, this.names[x]);
	editor.putLong("score"+x, this.score[x]);
	}
	editor.commit();
	return position;

	}

}
