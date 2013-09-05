/*
    HoloIRC - an IRC client for Android

    Copyright 2013 Lalit Maganti

    This file is part of HoloIRC.

    HoloIRC is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    HoloIRC is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with HoloIRC. If not, see <http://www.gnu.org/licenses/>.
 */

package com.fusionx.lightirc.irc;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.fusionx.lightirc.R;
import com.fusionx.lightirc.constants.EventBundleKeys;
import com.fusionx.lightirc.constants.ServerChannelEventTypeEnum;
import com.fusionx.lightirc.constants.ServerEventTypeEnum;
import com.fusionx.lightirc.interfaces.IIRCSideHandler;
import com.fusionx.lightirc.irc.connection.ConnectionWrapper;
import com.fusionx.lightirc.irc.writers.ServerWriter;
import com.fusionx.lightirc.uiircinterface.MessageSender;
import com.fusionx.lightirc.util.IRCUtils;
import com.fusionx.lightirc.util.MiscUtils;

import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.Iterator;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NonNull;
import lombok.Setter;

@Data
public class Server implements IIRCSideHandler {
    private ServerWriter writer;
    private UserChannelInterface userChannelInterface;

    private final String title;
    private AppUser user;

    @Setter(AccessLevel.NONE)
    private String buffer = "";
    private String status = "Disconnected";
    private String MOTD = "";

    private final ConnectionWrapper mWrapper;
    private final Context mContext;

    private final Handler serverHandler = new Handler() {
        @Override
        public void handleMessage(final Message msg) {
            final Bundle bundle = msg.getData();
            final Serializable serializable = bundle.getSerializable
                    (EventBundleKeys.eventType);
            if (serializable instanceof ServerEventTypeEnum) {
                final ServerEventTypeEnum type = (ServerEventTypeEnum) serializable;
                switch (type) {
                    case NickInUse:
                    case Generic:
                        buffer += bundle.getString(EventBundleKeys.message) + "\n";
                        break;
                }
            } else if (serializable instanceof ServerChannelEventTypeEnum) {
                final ServerChannelEventTypeEnum type = (ServerChannelEventTypeEnum) serializable;
                switch (type) {
                    case FinalDisconnected:
                        MessageSender.getSender(title).unregisterIRCSideHandlerInterface(title);
                    case RetryPendingDisconnected:
                    case Connected:
                    case SwitchToServerMessage:
                        buffer += bundle.getString(EventBundleKeys.message) + "\n";
                        break;
                }
            }
        }
    };

    public Server(final String serverTitle, final ConnectionWrapper wrapper, final Context context) {
        title = serverTitle;
        mWrapper = wrapper;
        mContext = context;

        MessageSender.getSender(serverTitle).registerIRCSideHandlerInterface(this);
    }

    public void privateMessageSent(final PrivateMessageUser userWhoIsNotUs, final String message,
                                   final boolean weAreSending) {
        final MessageSender sender = MessageSender.getSender(title);
        final User sendingUser = weAreSending ? user : userWhoIsNotUs;
        if (!user.isPrivateMessageOpen(userWhoIsNotUs)) {
            user.createPrivateMessage(userWhoIsNotUs);

            if (StringUtils.isNotEmpty(message)) {
                sender.sendPrivateMessage(userWhoIsNotUs.getNick(), sendingUser, message);
            }

            sender.sendNewPrivateMessage(userWhoIsNotUs.getNick());
        } else {
            if (StringUtils.isNotEmpty(message)) {
                sender.sendPrivateMessage(userWhoIsNotUs.getNick(), sendingUser, message);
            }
        }
    }

    public void privateActionSent(final PrivateMessageUser userWhoIsNotUs, final String action,
                                  final boolean weAreSending) {
        final MessageSender sender = MessageSender.getSender(title);
        final User sendingUser = weAreSending ? user : userWhoIsNotUs;
        if (!user.isPrivateMessageOpen(userWhoIsNotUs)) {
            user.createPrivateMessage(userWhoIsNotUs);

            if (StringUtils.isNotEmpty(action)) {
                sender.sendPrivateAction(userWhoIsNotUs.getNick(), sendingUser, action);
            }

            sender.sendNewPrivateMessage(userWhoIsNotUs.getNick());
        } else {
            if (StringUtils.isNotEmpty(action)) {
                sender.sendPrivateAction(userWhoIsNotUs.getNick(), sendingUser, action);
            }
        }
    }

    public synchronized PrivateMessageUser getPrivateMessageUser(@NonNull final String nick) {
        final Iterator<PrivateMessageUser> iterator = user.getPrivateMessageIterator();
        while (iterator.hasNext()) {
            final PrivateMessageUser privateMessageUser = iterator.next();
            if (IRCUtils.areNicksEqual(privateMessageUser.getNick(), nick)) {
                return privateMessageUser;
            }
        }
        return new PrivateMessageUser(nick, userChannelInterface);
    }

    @Override
    public Handler getChannelHandler(String channelName) {
        return userChannelInterface.getChannel(channelName).getChannelHandler();
    }

    @Override
    public Handler getUserHandler(String userNick) {
        return getPrivateMessageUser(userNick).getUserHandler();
    }

    @Override
    public String getNick() {
        return getUser().getNick();
    }

    public boolean isConnected(final Context context) {
        return status.equals(context.getString(R.string.status_connected));
    }

    public void disconnectFromServer(final Context context) {
        mWrapper.disconnectFromServer(context);
    }

    @Override
    public String toString() {
        return "HoloIRC " + MiscUtils.getAppVersion(mContext) + " Android IRC client";
    }
}