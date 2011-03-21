/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 2.0.2
 *
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */

package org.doubango.tinyWRAP;

public enum tdav_codec_id_t {
  tdav_codec_id_none(0x00000000),
  tdav_codec_id_amr_nb_oa(0x00000001 << 0),
  tdav_codec_id_amr_nb_be(0x00000001 << 1),
  tdav_codec_id_amr_wb_oa(0x00000001 << 2),
  tdav_codec_id_amr_wb_be(0x00000001 << 3),
  tdav_codec_id_gsm(0x00000001 << 4),
  tdav_codec_id_pcma(0x00000001 << 5),
  tdav_codec_id_pcmu(0x00000001 << 6),
  tdav_codec_id_ilbc(0x00000001 << 7),
  tdav_codec_id_speex_nb(0x00000001 << 8),
  tdav_codec_id_speex_wb(0x00000001 << 9),
  tdav_codec_id_speex_uwb(0x00000001 << 10),
  tdav_codec_id_bv16(0x00000001 << 11),
  tdav_codec_id_bv32(0x00000001 << 12),
  tdav_codec_id_evrc(0x00000001 << 13),
  tdav_codec_id_g729ab(0x00000001 << 14),
  tdav_codec_id_h261(0x00010000 << 0),
  tdav_codec_id_h263(0x00010000 << 1),
  tdav_codec_id_h263p(0x00010000 << 2),
  tdav_codec_id_h263pp(0x00010000 << 3),
  tdav_codec_id_h264_bp10(0x00010000 << 4),
  tdav_codec_id_h264_bp20(0x00010000 << 5),
  tdav_codec_id_h264_bp30(0x00010000 << 6),
  tdav_codec_id_theora(0x00010000 << 7),
  tdav_codec_id_mp4ves_es(0x00010000 << 8);

  public final int swigValue() {
    return swigValue;
  }

  public static tdav_codec_id_t swigToEnum(int swigValue) {
    tdav_codec_id_t[] swigValues = tdav_codec_id_t.class.getEnumConstants();
    if (swigValue < swigValues.length && swigValue >= 0 && swigValues[swigValue].swigValue == swigValue)
      return swigValues[swigValue];
    for (tdav_codec_id_t swigEnum : swigValues)
      if (swigEnum.swigValue == swigValue)
        return swigEnum;
    throw new IllegalArgumentException("No enum " + tdav_codec_id_t.class + " with value " + swigValue);
  }

  @SuppressWarnings("unused")
  private tdav_codec_id_t() {
    this.swigValue = SwigNext.next++;
  }

  @SuppressWarnings("unused")
  private tdav_codec_id_t(int swigValue) {
    this.swigValue = swigValue;
    SwigNext.next = swigValue+1;
  }

  @SuppressWarnings("unused")
  private tdav_codec_id_t(tdav_codec_id_t swigEnum) {
    this.swigValue = swigEnum.swigValue;
    SwigNext.next = this.swigValue+1;
  }

  private final int swigValue;

  private static class SwigNext {
    private static int next = 0;
  }
}

