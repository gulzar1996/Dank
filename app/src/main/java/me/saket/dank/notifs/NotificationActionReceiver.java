package me.saket.dank.notifs;

import static me.saket.dank.utils.RxUtils.doNothingCompletable;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.RemoteInput;
import android.widget.Toast;

import net.dean.jraw.models.Message;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Completable;
import me.saket.dank.R;
import me.saket.dank.di.Dank;
import me.saket.dank.utils.JacksonHelper;
import me.saket.dank.utils.JrawUtils;
import timber.log.Timber;

/**
 * Receives actions made on unread message notifications, generated by {@link MessagesNotificationManager}.
 */
public class NotificationActionReceiver extends BroadcastReceiver {

  public static final String KEY_DIRECT_REPLY_MESSAGE = "directReplyMessage";
  private static final String KEY_MESSAGE_JSON = "message";
  private static final String KEY_MESSAGE_ID = "messageId";
  private static final String KEY_MESSAGE_ID_LIST = "messageIdList";
  private static final String KEY_NOTIFICATION_ID = "notificationId";

  private static final String ACTION_MARK_AS_READ = "markAsRead";
  private static final String ACTION_DIRECT_REPLY = "quickReply";
  private static final String ACTION_MARK_AS_SEEN = "markAsSeen";
  private static final String ACTION_MARK_ALL_AS_SEEN = "markAllAsSeen";

  public static Intent createMarkAsReadIntent(Context context, Message message, JacksonHelper jacksonHelper) {
    Intent intent = new Intent(context, NotificationActionReceiver.class);
    intent.setAction(ACTION_MARK_AS_READ);
    intent.putExtra(KEY_MESSAGE_JSON, jacksonHelper.toJson(message));
    return intent;
  }

  /**
   * @param notificationId Used for dismissing the notification once the reply has been made.
   */
  public static Intent createDirectReplyIntent(Context context, Message replyToMessage, int notificationId, JacksonHelper jacksonHelper) {
    Intent intent = new Intent(context, NotificationActionReceiver.class);
    intent.setAction(ACTION_DIRECT_REPLY);
    intent.putExtra(KEY_MESSAGE_JSON, jacksonHelper.toJson(replyToMessage));
    intent.putExtra(KEY_NOTIFICATION_ID, notificationId);
    return intent;
  }

  /**
   * Gets called when an individual notification is dismissed.
   */
  public static Intent createMarkAsSeenIntent(Context context, Message messageToMarkAsSeen) {
    Intent intent = new Intent(context, NotificationActionReceiver.class);
    intent.setAction(ACTION_MARK_AS_SEEN);
    intent.putExtra(KEY_MESSAGE_ID, messageToMarkAsSeen.getId());
    return intent;
  }

  /**
   * Gets called when the entire bundled notification is dismissed.
   */
  public static Intent createMarkAllAsSeenIntent(Context context, List<Message> messagesToMarkAsSeen) {
    ArrayList<String> messageIdsToMarkAsSeen = new ArrayList<>(messagesToMarkAsSeen.size());
    for (Message message : messagesToMarkAsSeen) {
      messageIdsToMarkAsSeen.add(message.getId());
    }

    Intent intent = new Intent(context, NotificationActionReceiver.class);
    intent.setAction(ACTION_MARK_ALL_AS_SEEN);
    intent.putStringArrayListExtra(KEY_MESSAGE_ID_LIST, messageIdsToMarkAsSeen);
    return intent;
  }

  @Override
  public void onReceive(Context context, Intent intent) {
    String messageJson = intent.getStringExtra(KEY_MESSAGE_JSON);
    final long startTime = System.currentTimeMillis();
    Message message = JrawUtils.parseMessageJson(messageJson, Dank.jackson());
    Timber.i("Deserialized msg in: %sms", System.currentTimeMillis() - startTime);

    switch (intent.getAction()) {
      case ACTION_MARK_AS_READ:
        Dank.messagesNotifManager().markMessageNotificationAsSeen(message)
            .andThen(Completable.fromAction(() -> NotificationActionsJobService.markAsRead(context, message, Dank.jackson())))
            .subscribe();
        break;

      case ACTION_DIRECT_REPLY:
        Dank.messagesNotifManager().markMessageNotificationAsSeen(message)
            .andThen(Dank.messagesNotifManager().dismissNotification(context, intent.getIntExtra(KEY_NOTIFICATION_ID, -1)))
            .andThen(Completable.fromAction(() -> {
              Bundle directReplyResult = RemoteInput.getResultsFromIntent(intent);
              String replyText = directReplyResult.getString(KEY_DIRECT_REPLY_MESSAGE);
              NotificationActionsJobService.sendDirectReply(context, message, Dank.jackson(), replyText);
            }))
            .subscribe(doNothingCompletable(), error -> {
              Timber.e(error, "Couldn't send direct reply");
              Toast.makeText(context, R.string.common_unknown_error_message, Toast.LENGTH_LONG).show();
            });
        break;

      case ACTION_MARK_AS_SEEN:
        String messageIdToMarkAsSeen = intent.getStringExtra(KEY_MESSAGE_ID);
        Dank.messagesNotifManager()
            .markMessageNotificationsAsSeen(messageIdToMarkAsSeen)
            .subscribe();
        break;

      case ACTION_MARK_ALL_AS_SEEN:
        List<String> messageIdsToMarkAsSeen = intent.getStringArrayListExtra(KEY_MESSAGE_ID_LIST);
        Dank.messagesNotifManager()
            .markMessageNotificationsAsSeen(messageIdsToMarkAsSeen.toArray(new String[messageIdsToMarkAsSeen.size()]))
            .subscribe();
        break;

      default:
        throw new UnsupportedOperationException("Unknown action: " + intent.getAction());
    }
  }
}
