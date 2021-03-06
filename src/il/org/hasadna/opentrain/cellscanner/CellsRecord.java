package il.org.hasadna.opentrain.cellscanner;

import android.os.Build;
import android.telephony.CellLocation;
import android.telephony.NeighboringCellInfo;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

class CellsRecord {
    public static final String RADIO_GSM = "gsm";
    public static final String RADIO_CDMA = "cdma";
    public static final String RADIO_WCDMA = "wcdma";

    public static final String CELL_RADIO_GSM = "gsm";
    public static final String CELL_RADIO_UMTS = "umts";
    public static final String CELL_RADIO_CDMA = "cdma";
    public static final String CELL_RADIO_LTE = "lte";

    private String mRadio = RADIO_GSM;
    private final List<CellInfo> mCells = new ArrayList<CellInfo>();

    public static class CellInfo {
        static final int UNKNOWN_CID = -1;
        static final int UNKNOWN_SIGNAL = -1000;

        private String mRadio;

        private int mMcc;
        private int mMnc;
        private int mCid;
        private int mLac;
        private int mSignal;
        private int mAsu;
        private int mTa;
        private int mPsc;

        CellInfo() {
            reset();
        }

        public String getRadio() {
            return mRadio;
        }

        public JSONObject toJSONObject() {
            final JSONObject obj = new JSONObject();

            try {
                obj.put("radio", getRadio());
                obj.put("mcc", mMcc);
                obj.put("mnc", mMnc);
                if (mLac != UNKNOWN_CID) obj.put("lac", mLac);
                if (mCid != UNKNOWN_CID) obj.put("cid", mCid);
                if (mSignal != UNKNOWN_SIGNAL) obj.put("signal", mSignal);
                if (mAsu != UNKNOWN_SIGNAL) obj.put("asu", mAsu);
                if (mTa != UNKNOWN_CID) obj.put("ta", mTa);
                if (mPsc != UNKNOWN_CID) obj.put("psc", mPsc);
            } catch (JSONException jsonE) {
                throw new IllegalStateException(jsonE);
            }

            return obj;
        }

        void reset() {
            mRadio = CELL_RADIO_GSM;
            mMcc = UNKNOWN_CID;
            mMnc = UNKNOWN_CID;
            mLac = UNKNOWN_CID;
            mCid = UNKNOWN_CID;
            mSignal = UNKNOWN_SIGNAL;
            mAsu = UNKNOWN_SIGNAL;
            mTa = UNKNOWN_CID;
            mPsc = UNKNOWN_CID;
        }

        void setCellLocation(CellLocation cl,
                             int networkType,
                             String networkOperator,
                             Integer gsmSignalStrength,
                             Integer cdmaRssi) {
            if (cl instanceof GsmCellLocation) {
                final int lac, cid;
                final GsmCellLocation gcl = (GsmCellLocation) cl;

                reset();
                mRadio = getCellRadioTypeName(networkType);
                setNetworkOperator(networkOperator);

                lac = gcl.getLac();
                cid = gcl.getCid();
                if (lac >= 0) mLac = lac;
                if (cid >= 0) mCid = cid;

                if (Build.VERSION.SDK_INT >= 9) {
                    final int psc = gcl.getPsc();
                    if (psc >= 0) mPsc = psc;
                }

                if (gsmSignalStrength != null) {
                    mAsu = gsmSignalStrength;
                }
            } else if (cl instanceof CdmaCellLocation) {
                final CdmaCellLocation cdl = (CdmaCellLocation) cl;

                reset();
                mRadio = getCellRadioTypeName(networkType);

                setNetworkOperator(networkOperator);

                mMnc = cdl.getSystemId();

                mLac = cdl.getNetworkId();
                mCid = cdl.getBaseStationId();

                if (cdmaRssi != null) {
                    mSignal = cdmaRssi;
                }
            } else {
                throw new IllegalArgumentException("Unexpected CellLocation type: " + cl.getClass().getName());
            }
        }

        void setNeighboringCellInfo(NeighboringCellInfo nci, String networkOperator) {
            final int lac, cid, psc, rssi;

            reset();
            mRadio = getCellRadioTypeName(nci.getNetworkType());
            setNetworkOperator(networkOperator);

            lac = nci.getLac();
            cid = nci.getCid();
            psc = nci.getPsc();
            rssi = nci.getRssi();

            if (lac >= 0) mLac = lac;
            if (cid >= 0) mCid = cid;
            if (psc >= 0) mPsc = psc;
            if (rssi != NeighboringCellInfo.UNKNOWN_RSSI) mAsu = rssi;
        }

        void setNetworkOperator(String mccMnc) {
            if (mccMnc == null || mccMnc.length() < 5 || mccMnc.length() > 8) {
                throw new IllegalArgumentException("Bad mccMnc: " + mccMnc);
            }
            mMcc = Integer.parseInt(mccMnc.substring(0, 3));
            mMnc = Integer.parseInt(mccMnc.substring(3));
        }

        static String getCellRadioTypeName(int networkType) {
            String name;
            switch (networkType) {
                case TelephonyManager.NETWORK_TYPE_GPRS:
                case TelephonyManager.NETWORK_TYPE_EDGE:
                    return CELL_RADIO_GSM;
                case TelephonyManager.NETWORK_TYPE_UMTS:
                case TelephonyManager.NETWORK_TYPE_HSDPA:
                case TelephonyManager.NETWORK_TYPE_HSUPA:
                case TelephonyManager.NETWORK_TYPE_HSPA:
                case TelephonyManager.NETWORK_TYPE_HSPAP:
                    return CELL_RADIO_UMTS;
                case TelephonyManager.NETWORK_TYPE_LTE:
                    return CELL_RADIO_LTE;
                case TelephonyManager.NETWORK_TYPE_EVDO_0:
                    name = "CDMA - EvDo rev. 0";
                    break;
                case TelephonyManager.NETWORK_TYPE_EVDO_A:
                    name = "CDMA - EvDo rev. A";
                    break;
                case TelephonyManager.NETWORK_TYPE_EVDO_B:
                    name = "CDMA - EvDo rev. B";
                    break;
                case TelephonyManager.NETWORK_TYPE_1xRTT:
                    name = "CDMA - 1xRTT";
                    break;
                case TelephonyManager.NETWORK_TYPE_EHRPD:
                    name = "CDMA - eHRPD";
                    break;
                case TelephonyManager.NETWORK_TYPE_IDEN:
                    name = "iDEN";
                    break;
                default:
                    name = "UNKNOWN (" + String.valueOf(networkType) + ")";
                    break;
            }
            throw new IllegalArgumentException("Unexpected Network Type: " + name);
        }
    }

    public CellsRecord(int phoneType) {
        setRadio(phoneType);
    }

    /**
     * @return {@link #RADIO_GSM}, {@link #RADIO_CDMA}, or {@link #RADIO_WCDMA}
     */
    public String getRadio() {
        return mRadio;
    }

    public boolean hasCells() {
        return !mCells.isEmpty();
    }

    public JSONArray getCellsAsJson() {
        JSONArray arr = new JSONArray();
        for (CellInfo cell : mCells) arr.put(cell.toJSONObject());
        return arr;
    }

    public void putCellLocation(CellLocation cl, int networkType,
                                String networkOperator, Integer gsmSignalStrength,
                                Integer cdmaRssi) {
        CellInfo ci = new CellInfo();
        ci.setCellLocation(cl, networkType, networkOperator, gsmSignalStrength, cdmaRssi);
        mCells.add(ci);
    }

    public void putNeighboringCell(NeighboringCellInfo neighbour,
                                   String networkOperator) {
        CellInfo ci = new CellInfo();
        ci.setNeighboringCellInfo(neighbour, networkOperator);
        mCells.add(ci);
    }

    void setRadio(int phoneType) {
        String radio = getRadioTypeName(phoneType);
        if (radio == null) {
            throw new IllegalArgumentException("Unexpected Phone Type: " + phoneType);
        }
        mRadio = radio;
    }

    private static String getRadioTypeName(int phoneType) {
        switch (phoneType) {
            case TelephonyManager.PHONE_TYPE_CDMA:
                return RADIO_CDMA;

            case TelephonyManager.PHONE_TYPE_GSM:
                return RADIO_GSM;

            case TelephonyManager.PHONE_TYPE_NONE:
            case TelephonyManager.PHONE_TYPE_SIP:
                // These devices have no radio.
            default:
                return null;
        }
    }
}
