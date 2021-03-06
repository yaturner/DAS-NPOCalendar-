package com.das.yacalendar.network;

import android.os.Message;

import com.das.yacalendar.Constants;
import com.das.yacalendar.calendar.CalendarInfo;
import com.das.yacalendar.yacalendar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by yaturner on 3/30/2015.
 */
public class InfoServerCall extends BasicAPICall
{

    public InfoServerCall(final yacalendar main)
    {
        super(main);
    }

    @Override
    protected Void doInBackground(String... params)
    {
        return super.doInBackground(params);
    }

    @Override
    protected void onPostExecute(Object result)
    {
        super.onPostExecute(result);
    }

    protected void parseResult(final String result)
    {
        int version = -1;
        String startDate = null;
        String endDate = null;
        CalendarInfo calendarInfo = null;

        if(result != null && result.length() > 0) {
            try {
                JSONArray resultObject = new JSONArray(result);
                JSONObject info = resultObject.getJSONObject(0);
                JSONArray infoArray = info.getJSONArray("info");
                JSONObject obj = infoArray.getJSONObject(0);
                String versionString = (String) obj.get("version");
                if (versionString != null && versionString.length() > 0) {
                    version = Integer.parseInt(versionString);
                } else {
                    version = -1;
                }
                obj = infoArray.optJSONObject(1);
                startDate = obj.getString("start_date");
                obj = infoArray.optJSONObject(2);
                endDate = obj.getString("end_date");
            } catch (JSONException e) {
                e.printStackTrace();
            }


            if (version > 0 && startDate != null && startDate.length() > 0 &&
                    endDate != null && endDate.length() > 0) {
                calendarInfo = new CalendarInfo(version, startDate, endDate);
            }
        }
        Message msg = main.msgHandler.obtainMessage(Constants.HANDLER_MESSAGE_INFO);
        msg.obj = (Object) calendarInfo;
        main.msgHandler.sendMessage(msg);

    }
}
