package com.library.inventory.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;

@Embeddable
public class Location {

    @Column(name = "library_code", nullable = false, length = 10)
    private String libraryCode;

    @Column(name = "floor")
    private Integer floor;

    @Column(name = "zone", length = 10)
    private String zone;

    @Column(name = "aisle", length = 10)
    private String aisle;

    @Column(name = "shelf", length = 10)
    private String shelf;

    @Column(name = "position", length = 10)
    private String position;

    @Column(name = "location_code", length = 50)
    private String locationCode;

    protected Location() {
    }

    public Location(String libraryCode, Integer floor, String zone, String aisle, String shelf, String position) {
        this.libraryCode = validateLibraryCode(libraryCode);
        this.floor = floor != null && floor < 0 ? null : floor;
        this.zone = zone;
        this.aisle = aisle;
        this.shelf = shelf;
        this.position = position;
        this.locationCode = generateLocationCode();
    }

    public static Location of(String libraryCode, Integer floor, String zone, String aisle, String shelf, String position) {
        return new Location(libraryCode, floor, zone, aisle, shelf, position);
    }

    public static Location simple(String libraryCode, String locationCode) {
        Objects.requireNonNull(libraryCode, "Library code must not be null");
        Location loc = new Location();
        loc.libraryCode = libraryCode.toUpperCase().trim();
        loc.locationCode = locationCode;
        loc.parseLocationCode(locationCode);
        return loc;
    }

    private String validateLibraryCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Library code must not be blank");
        }
        return code.toUpperCase().trim();
    }

    private String generateLocationCode() {
        String zoneStr = (zone != null && !zone.isBlank()) ? zone : "A";
        String aisleStr = (aisle != null && !aisle.isBlank()) ? aisle : "01";
        String shelfStr = (shelf != null && !shelf.isBlank()) ? shelf : "A";
        String posStr = (position != null && !position.isBlank()) ? position : "001";
        return String.format("%s%s-%s-%s", zoneStr, aisleStr, shelfStr, posStr);
    }

    private void parseLocationCode(String code) {
        if (code == null) return;
        String[] parts = code.split("-");
        if (parts.length >= 3) {
            String zoneAisle = parts[0];
            if (!zoneAisle.isEmpty()) {
                this.zone = zoneAisle.substring(0, 1);
                this.aisle = zoneAisle.length() > 1 ? zoneAisle.substring(1) : "01";
            }
            this.shelf = parts[1];
            this.position = parts[2];
        }
    }

    public String getFullDescription() {
        return String.format("Library %s, Floor %s, Zone %s, Aisle %s, Shelf %s, Position %s",
            libraryCode, floor, zone, aisle, shelf, position);
    }

    public String getLibraryCode() { return libraryCode; }
    public Integer getFloor() { return floor; }
    public String getZone() { return zone; }
    public String getAisle() { return aisle; }
    public String getShelf() { return shelf; }
    public String getPosition() { return position; }
    public String getLocationCode() { return locationCode; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Location location = (Location) o;
        return Objects.equals(libraryCode, location.libraryCode) &&
               Objects.equals(locationCode, location.locationCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(libraryCode, locationCode);
    }

    @Override
    public String toString() {
        return locationCode;
    }
}
