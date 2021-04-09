#RAM2CAT converter

Converts V.V.Ilyushin's RAM codes (RAM36, RAM36hf, RAM_C2v_2tops) output files into SPCAT .cat format

###Version 1.1 (09/04/2021)
- corrected error in MHz to cm-1 conversion
- corrected reading the molecular tag field
- added conversion from RAM36 two-top "catalog" format to .cat:
  - torsional states are represented by usual v quantum number
  - the AA, EE, EA, and AE substates are represented by the fifth quantum number 
  v' with the designations of 0, 1, 3, and 5, respectively.

###Version 1.0 (05/04/2021)
- works with "catalog" and "standard" RAM36 and RAM36hf outputs
- correct conversion for J or F up to 99
- runs under **Java 1.8.x**