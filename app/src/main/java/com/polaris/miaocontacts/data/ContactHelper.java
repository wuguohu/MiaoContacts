package com.polaris.miaocontacts.data;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by rossie on 2019/2/1.
 */

public class ContactHelper {

    public List<ContactInfo> getContacts(Context context) throws Exception {
        List<ContactInfo> contactInfos = new ArrayList<>();


        //获取联系人信息的Uri
        Uri uri = ContactsContract.Contacts.CONTENT_URI;
        //获取ContentResolver
        ContentResolver contentResolver = context.getContentResolver();
        //查询数据，返回Cursor
        Cursor cursor = contentResolver.query(uri, null, null, null, null);
        while (cursor.moveToNext()) {
            ContactInfo info = new ContactInfo();
            StringBuilder sb = new StringBuilder();
            //获取联系人的ID
            String contactId = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
            //获取联系人的姓名
            String name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));

            info.setId(contactId);
            info.setName(name);


            //查询电话类型的数据操作
            Cursor phones = contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    null,
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + contactId,
                    null, null);
            while (phones.moveToNext()) {
                String phoneNumber = phones.getString(phones.getColumnIndex(
                        ContactsContract.CommonDataKinds.Phone.NUMBER));
                //添加Phone的信息
                if(phoneNumber != null){
                    info.setPhone(phoneNumber.replace("-", ""));
                }
            }
            phones.close();

            contactInfos.add(info);
        }
        cursor.close();

        return contactInfos;
    }

    public void deleteContacts(Context context, String rawContactId) throws Exception {
        context.getContentResolver().delete(
                ContentUris.withAppendedId(ContactsContract.RawContacts.CONTENT_URI,
                        Long.valueOf(rawContactId)), null, null);
    }
}
