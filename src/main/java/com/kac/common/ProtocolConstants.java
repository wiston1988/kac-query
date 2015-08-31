package com.kac.common;

public class ProtocolConstants
{
  public static final long MAGIC = MarkerVersion.makeLong(MarkerVersion.MARKER, MarkerVersion.VERSION);
  public static final short RC_RS_JOIN = 501;
  public static final short RC_RS_JOIN_SUCC = 502;
  public static final short RC_RS_JOIN_FAIL = 503;
  public static final short RC_RS_MULTIREQUEST = 504;
  public static final short RC_RS_MULTIREQUEST_SUCC = 505;
  public static final short RC_RS_MULTIREQUEST_FAIL = 506;
  public static final int HEADSIZE = 22;
}
