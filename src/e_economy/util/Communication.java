package e_economy.util;

import aic2024.user.ActionType;
import aic2024.user.BroadcastInfo;
import aic2024.user.UnitController;

/**
 * Handle communication via broadcasting queue.
 * A broadcast message from an HQ is composed of one or more integers in the queue.
 * The first integer stores the message type, round it was sent, ID of the intended recipient, and length of the message
 *  (not including the first integer).
 * The remaining integers are dependent on the message type.
 * <br>
 * A broadcast message from an astronaut is one integer. The length is replaced with the paylaod.
 */

public class Communication {
    private final UnitController uc;

    public Communication(UnitController uc) {
        this.uc = uc;
    }

    private final int TYPES = 3;
    public final int DIR_SCORES = 0;
    public final int ASPHYXIATED = 1;
    public final int FOUND_ENEMY_HQ = 2;

    private final int MAX_ROUND = 1005;
    private final int MAX_ID = 10005;  // last bit represents whether we want the astronaut's ID or its parent's ID
    private final int MAX_LENGTH = 50;
    public final int MAX_PAYLOAD = MAX_LENGTH;

    /**
     * Read in a single broadcast message, consisting of one or more integers.
     * Returns [pack(type, round, recipient ID * 2 + parentIsTarget, length), additional integers...]
     */
    private int[] readBroadcastFromHq() {
        final BroadcastInfo info = uc.pollBroadcast();
        if (info == null) {
//            uc.println("readBroadcastFromHq null");
            return null;
        }

        final int msg = info.getMessage();
        if (typeOf(msg) != DIR_SCORES) {
            return new int[]{msg};
        }

        final int len = msg % MAX_LENGTH;
//        uc.println("readBroadcastFromHq " + msg + " " + typeOf(msg) + " " + len);
        final int[] parsed = new int[len + 1];
        parsed[0] = msg;

        // Need to loop in order for this one. Assume that messages haven't been corrupted; investigate here if NPE happens
        for (int i = 1; i <= len; ++i) parsed[i] = uc.pollBroadcast().getMessage();
        return parsed;
    }

    private int readBroadcastFromAstronaut() {
        final BroadcastInfo info = uc.pollBroadcast();
        if (info == null) {
            return -1;
        }
        return info.getMessage();
    }

    /**
     * Processes communications for an HQ.
     * Reads and returns any messages from astronauts, sends new messages, and puts back messages for other recipients.
     * Stale messages sent by HQs will be deleted.
     * This method will empty out the broadcast buffer before replaying messages as necessary.
     * <br>
     * Takes a single message to send and an array of messages to store the received messages.
     * Returns the number of received messages.
     * <br>
     * THIS SHOULD ONLY BE INVOKED BY STRUCTURES.
     */
    public int receiveMessagesFromAstronauts(int[] messagesReceived) {
//        uc.println("receiveMessagesFromAstronauts start");
        int n_recv = 0;
        messagesReceived[n_recv] = readBroadcastFromAstronaut();
        while (messagesReceived[n_recv] != -1) {
//            uc.println("receivedAstro " + messagesReceived[n_recv] + " " + typeOf(messagesReceived[n_recv]) + " " + (messagesReceived[n_recv] / MAX_LENGTH) % MAX_ID);
            if (isRecipient(messagesReceived[n_recv])) ++n_recv;
            messagesReceived[n_recv] = readBroadcastFromAstronaut();
        }
        return n_recv;
    }

    /**
     * Receives a message from parent HQ
     */
    public int[] receiveDirectionsMessage() {
        int[] message = readBroadcastFromHq();
        while (message != null) {
            if (isRecipient(message[0]) && typeOf(message[0]) == DIR_SCORES) return message;
            message = readBroadcastFromHq();
        }
        return null;
    }

    /**
     * Broadcasts a single message
     */
    public void broadcastMessage(int[] message) {
//        uc.println("broadcast " + Arrays.toString(message));
        for (int i = 0; i < message.length; ++i) {
            uc.performAction(ActionType.BROADCAST, null, message[i]);
        }
    }

    public int[] dirScoresMessage(int[] dirScores) {
//        uc.println("dirScoresMessage " + uc.getRound() + " " + uc.getID() + " " + dirScores.length);
        final int[] message = new int[dirScores.length + 1];
        message[0] = packInfo(DIR_SCORES, uc.getRound(), uc.getID(), dirScores.length);
        for (int i = dirScores.length; i --> 0;) message[i + 1] = dirScores[i];
//        uc.println(Arrays.toString(message));
        return message;
    }

    public int[] asphyxiatedMessage(int dir) {
//        uc.println("asphyxiatedMessage " + uc.getRound() + " " + uc.getID() + " " + dir);
        final int[] message = new int[2];
        message[0] = packInfo(ASPHYXIATED, uc.getRound(), uc.getParent().getID(), dir);
        message[1] = dir;
        return message;
    }

    private int packInfo(int type, int round, int recipient, int length) {
        return ((type * MAX_ROUND + round) * MAX_ID + recipient) * MAX_LENGTH + length;
    }

    private boolean isRecipient(int message) {
        final int recipient = (message / MAX_LENGTH) % MAX_ID;
        return recipient == uc.getID() || recipient == uc.getParent().getID();
    }

    public int typeOf(int message) {
        return ((message / MAX_LENGTH) / MAX_ID) / MAX_ROUND;
    }
}
