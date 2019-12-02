# GNM-DESk: Differential Equations Solver in Kotlin
Door Cedric De Donder, 2019-2020.

Dit is de repo voor Project 3 van het vak Gevorderde Numerieke Methoden (UGent) voor prof. dr. M. Van Daele.

## Gebruik

De applicatie kan [hier](https://github.com/cshdedonder/GNM-DESk/blob/master/desk.jar?raw=true) gevonden worden. Als jar kan dit via de terminal geopend worden:
```bash
java -jar desk.jar
```
Omdat één van de afhankelijkheden van een gebruikte bibliotheek niet geüpdated werd naar Java 9, verschijnen er bij het uitvoeren in Java 9+ omgevingen een aantal waarschuwingen; deze zijn te negeren.

Bij het opstarten wordt een venster getoond waarop de probleemspecificatie ingevuld kan worden.
Na op `Calculate` te klikken, verschijnt er eveneens een venster waarop de numerieke oplossing van het probleem getoond wordt en eventueel gemanipuleerd kan worden. Een screenshot kan genomen worden met een toetsaanslag `s`. 

## Referenties

De volgende bibliotheken werden in het project gebruikt:
 + [EvalEx](https://github.com/uklimaschewski/EvalEx) door Udo Klimaschewski
 + [jzy3d](http://www.jzy3d.org/)
 + [Apache Commons Math 3](http://commons.apache.org/proper/commons-math/)
 + [TornadoFX](https://tornadofx.io/)
 + [JavaFX](https://openjfx.io/)
