// SINGLE VALUED CONTROLS

CWCc : CWControl {
    // A MIDI CC control, mapped to the range [0..1]

    // Updates from other connected controls are sent back to the same CC so
    // that devices can update thier sense of where the setting is. This works
    // for encoder based devices, moterized faders, and controllers that
    // implement "pick up" mode.

    classvar stdSpec;
    var midiFunc, midiSend, spec;

    *initClass {
        Class.initClassTree(Warp);
        stdSpec = ControlSpec.new(0, 127, \lin, 1);
    }
    *new { |point, devId, midiOut, ch, cc, spec=nil, unmapped=false |
        ^super.new().initCc(point, devId, midiOut, ch, cc, spec, unmapped)
    }
    initCc { |point, devId, midiOut, ch, cc, specArg, unmapped|
        // the spec maps from "patch value" to MIDI's [0..127]
        spec = specArg ?? { if (unmapped) { nil } { stdSpec } };

        if (devId.isNil.not) {
            midiFunc = MIDIFunc.cc(
                { |cv| this.send(\set, spec !? _.unmap(cv) ? cv) },
                cc, ch, devId
            );
        };
        if (midiOut.isNil.not) {
            midiSend = { |cv| midiOut.control(ch, cc, cv) };
        };

        this.connect(point);
    }
    free {
        midiFunc.free;
        midiFunc = nil;
        midiSend = nil;
        super.free;
    }

    set { |v| midiSend !? _.(spec !? _.map(v) ? v) }
}

CWBend : CWControl {
    // MIDI PitchBend

    classvar stdSpec;
    var midiFunc, midiSend, spec;

    *initClass {
        Class.initClassTree(Warp);
        stdSpec = ControlSpec.new(0, 16r3fff, \lin, 1);
    }
    *new { |point, devId, midiOut, ch, spec=nil, unmapped=false |
        ^super.new().initBend(point, devId, midiOut, ch, spec, unmapped)
    }
    initBend { |point, devId, midiOut, ch, specArg, unmapped|
        spec = specArg ?? { if (unmapped) { nil } { stdSpec } };

        if (devId.isNil.not) {
            midiFunc = MIDIFunc.bend(
                { |cv| this.send(\set, spec !? _.unmap(cv) ? cv) },
                ch, devId
            );
        };
        if (midiOut.isNil.not) {
            midiSend = { |cv| midiOut.bend(ch, cv) };
        };

        this.connect(point);
    }
    free {
        midiFunc.free;
        midiFunc = nil;
        midiSend = nil;
        super.free;
    }

    set { |v| midiSend !? _.(spec !? _.map(v) ? v) }
}

CWEncoder : CWControl {
    // A MIDI CC control from an encoder

    var midiFunc, midiSend, lastValue, increment;

    *new { |point, devId, midiOut, ch, cc, increment=1 |
        ^super.new().initEncoder(point, devId, midiOut, ch, cc, increment)
    }
    initEncoder { |point, devId, midiOut, ch, cc, incrementArg|
        increment = incrementArg;

        if (devId.isNil.not) {
            midiFunc = MIDIFunc.cc(
                { |cv| this.adjust(cv) },
                cc, ch, devId
            );
        };

        this.connect(point);
    }
    free {
        midiFunc.free;
        midiFunc = nil;
        midiSend = nil;
        super.free;
    }

    set { |v| lastValue = v; }
    adjust { |cv|
        var delta = increment * (if (cv <= 64) { cv } { cv - 128 });

        lastValue = lastValue !? (_ + delta);
        lastValue !? this.send(\set, _);
    }
}



CWProgram : CWControl {
    // Just the setting of a program change.
    var midiFunc, midiSend;

    *new { |point, devId, midiOut, ch|
        ^super.new().initProgram(point, devId, midiOut, ch)
    }
    initProgram { |point, devId, midiOut, ch|
        if (devId.isNil.not) {
            midiFunc = MIDIFunc.program(
                { |num| this.send(\trigger, num) },
                ch, devId
            );
        };
        if (midiOut.isNil.not) {
            midiSend = { |num| midiOut.program(ch, num) };
        };

        this.connect(point);
    }
    free {
        midiFunc.free;
        midiFunc = nil;
        midiSend = nil;
        super.free;
    }

    trigger { |v| midiSend !? _.(v) }
}



