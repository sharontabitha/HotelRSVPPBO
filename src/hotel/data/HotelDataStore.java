package hotel.data;

import hotel.model.HotelData;

import java.io.IOException;

public interface HotelDataStore {
    HotelData load() throws IOException;
    void save(HotelData data) throws IOException;
    String getLocation();
}
