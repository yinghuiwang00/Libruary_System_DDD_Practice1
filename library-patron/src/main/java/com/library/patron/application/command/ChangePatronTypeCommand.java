package com.library.patron.application.command;

import com.library.patron.domain.model.enums.PatronType;
import com.library.shared.domain.model.PatronId;

public class ChangePatronTypeCommand {

    private PatronId patronId;
    private PatronType newPatronType;

    public ChangePatronTypeCommand() {
    }

    public ChangePatronTypeCommand(PatronId patronId, PatronType newPatronType) {
        this.patronId = patronId;
        this.newPatronType = newPatronType;
    }

    public PatronId getPatronId() { return patronId; }
    public void setPatronId(PatronId patronId) { this.patronId = patronId; }
    public PatronType getNewPatronType() { return newPatronType; }
    public void setNewPatronType(PatronType newPatronType) { this.newPatronType = newPatronType; }
}
