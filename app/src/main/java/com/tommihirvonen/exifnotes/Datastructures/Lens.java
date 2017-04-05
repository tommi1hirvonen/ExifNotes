package com.tommihirvonen.exifnotes.Datastructures;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Lens class holds the information of one lens.
 */
public class Lens implements Parcelable {

    /**
     * database id
     */
    private long id;

    /**
     * make/manufacturer
     */
    private String make;

    /**
     * model
     */
    private String model;

    /**
     * lens's serial number, can contain letters
     */
    private String serialNumber;

    /**
     * lens's minimum aperture (highest f-number), number only
     */
    private String minAperture;

    /**
     * lens's maximum aperture (lowest f-number), number only
     */
    private String maxAperture;

    /**
     * lens's minimum focal length
     */
    private int minFocalLength;

    /**
     * lens's maximum focal length
     */
    private int maxFocalLength;

    /**
     *  integer defining whether the aperture values can be changed in
     *  third, half or full stop increments
     *
     * 0 = third stop (default)
     * 1 = half stop
     * 2 = full stop
     */
    private int apertureIncrements = 0;


    /**
     * Empty constructor
     */
    public Lens(){

    }

    /**
     * constructor to initialize all members
     *
     * @param id database id
     * @param make make/manufacturer
     * @param model model
     * @param serialNumber lens's serial number
     * @param minAperture minimum aperture value (highest f-number)
     * @param maxAperture maximum aperture value (lowest f-number)
     * @param minFocalLength minimum focal length
     * @param maxFocalLength maximum focal length
     * @param apertureIncrements aperture value change increment
     */
    public Lens(long id, String make, String model, String serialNumber, String minAperture,
                String maxAperture, int minFocalLength, int maxFocalLength, int apertureIncrements){
        this.id = id;
        this.make = make;
        this.model = model;
        this.serialNumber = serialNumber;
        this.minAperture = minAperture;
        this.maxAperture = maxAperture;
        this.minFocalLength = minFocalLength;
        this.maxFocalLength = maxFocalLength;
        if (apertureIncrements <= 2 && apertureIncrements >= 0){
            this.apertureIncrements = apertureIncrements;
        }
    }

    // GETTERS AND SETTERS

    /**
     *
     * @param input database id
     */
    public void setId(long input){
        this.id = input;
    }

    /**
     *
     * @param input make/manufacturer
     */
    public void setMake(String input){
        this.make = input;
    }

    /**
     *
     * @param input model
     */
    public void setModel(String input){
        this.model = input;
    }

    /**
     *
     * @param input serial number
     */
    public void setSerialNumber(String input){
        this.serialNumber = input;
    }

    /**
     *
     * @param input minimum aperture (highest f-number), number only
     */
    public void setMinAperture(String input){
        this.minAperture = input;
    }

    /**
     *
     * @param input maximum aperture (lowest f-number), number only
     */
    public void setMaxAperture(String input){
        this.maxAperture = input;
    }

    /**
     *
     * @param input minimum focal length
     */
    public void setMinFocalLength(int input){
        this.minFocalLength = input;
    }

    /**
     *
     * @param input maximum focal length
     */
    public void setMaxFocalLength(int input){
        this.maxFocalLength = input;
    }

    /**
     *
     * @param input aperture value change increments
     *              0 = third stop (default)
     *              1 = half stop
     *              2 = full stop
     */
    public void setApertureIncrements(int input){
        if (input <= 2 && input >= 0){
            this.apertureIncrements = input;
        }
    }

    /**
     *
     * @return database id
     */
    public long getId(){
        return this.id;
    }

    /**
     *
     * @return make/manufacturer
     */
    public String getMake(){
        return this.make;
    }

    /**
     *
     * @return model
     */
    public String getModel(){
        return this.model;
    }

    /**
     *
     * @return serial number
     */
    public String getSerialNumber(){
        return this.serialNumber;
    }

    /**
     *
     * @return minimum aperture (highest f-number), number only
     */
    public String getMinAperture(){
        return this.minAperture;
    }

    /**
     *
     * @return maximum aperture (lowest f-number), number only
     */
    public String getMaxAperture(){
        return this.maxAperture;
    }

    /**
     *
     * @return minimum focal length
     */
    public int getMinFocalLength(){
        return this.minFocalLength;
    }

    /**
     *
     * @return maximum focal length
     */
    public int getMaxFocalLength(){
        return this.maxFocalLength;
    }

    /**
     *
     * @return aperture value change increments
     *              0 = third stop (default)
     *              1 = half stop
     *              2 = full stop
     */
    public int getApertureIncrements(){
        return this.apertureIncrements;
    }


    //METHODS TO IMPLEMENT THE PARCELABLE CLASS TO PASS OBJECT INSIDE INTENTS

    /**
     * Constructs object from Parcel
     *
     * @param pc parcel object containing Camera's information
     */
    public Lens(Parcel pc){
        this.id = pc.readLong();
        this.make = pc.readString();
        this.model = pc.readString();
        this.serialNumber = pc.readString();
        this.minAperture = pc.readString();
        this.maxAperture = pc.readString();
        this.minFocalLength = pc.readInt();
        this.maxFocalLength = pc.readInt();
        this.apertureIncrements = pc.readInt();
    }

    /**
     * Not used
     *
     * @return not used
     */
    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Writes this object's members to a Parcel given as argument
     *
     * @param parcel Parcel which should be written with this object's members
     * @param i not used
     */
    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeLong(id);
        parcel.writeString(make);
        parcel.writeString(model);
        parcel.writeString(serialNumber);
        parcel.writeString(minAperture);
        parcel.writeString(maxAperture);
        parcel.writeInt(minFocalLength);
        parcel.writeInt(maxFocalLength);
        parcel.writeInt(apertureIncrements);
    }

    /**
     * Static field used to regenerate object, individually or as arrays
     */
    public static final Parcelable.Creator<Lens> CREATOR = new Parcelable.Creator<Lens>() {
        public Lens createFromParcel(Parcel pc) {
            return new Lens(pc);
        }
        public Lens[] newArray(int size) {
            return new Lens[size];
        }
    };

    /**
     * Custom equals to compare two Lenses
     *
     * @param obj Lens object
     * @return true if obj is Lens and all its members equal to this object's members
     */
    @Override
    public boolean equals(Object obj) {
        Lens lens;
        if (obj instanceof Lens) lens = (Lens) obj;
        else return false;
        return lens.getId() == id && lens.getMake().equals(make) &&
                lens.getModel().equals(model) && lens.getSerialNumber().equals(serialNumber) &&
                lens.getMaxAperture().equals(maxAperture) && lens.getMinAperture().equals(minAperture) &&
                lens.getApertureIncrements() == apertureIncrements &&
                lens.getMinFocalLength() == minFocalLength && lens.getMaxFocalLength() == maxFocalLength;
    }
}
