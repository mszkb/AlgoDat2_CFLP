package ad2.ss17.cflp;

import java.util.*;

/**
 * Klasse zum Berechnen der L&ouml;sung mittels Branch-and-Bound.
 * Hier sollen Sie Ihre L&ouml;sung implementieren.
 */
public class CFLP extends AbstractCFLP {

    private CFLPInstance cflp;

    //First integer stores customer id
    //array stores distance to each facility
    private ArrayList<KeyValue[]> shortestCustomerToFacility = new ArrayList<>();

    //global upper an lower bound
    private int upperBound = Integer.MAX_VALUE;

    int[] greedyAllocation;
    boolean[] facilityFixed;



    public CFLP(CFLPInstance instance) {
        this.cflp = instance;
        this.greedyAllocation = new int[this.cflp.getNumCustomers()];
        this.facilityFixed = new boolean[this.cflp.getNumFacilities()];

        // Idee:
        //
        // Erstelle eine sortierte Liste von Kunden mit den Distanzkosten,
        // Sortiert nach Distanzkosten.
        // Hierbei wird die Klasse KeyValue erstellt, welche die Facility
        // Nummer speichert
        sortiereFacilityKosten();

        this.greedyAllocation = initialFill();
        this.upperBound = calculateUpperBound(this.greedyAllocation);
    }

    /**
     * Sortiert die kosten der Distanzen aller Kunden zu der jeweiligen Facility aufsteigend
     */
    private void sortiereFacilityKosten() {

        //=============== Variablen - START ==//

        int n = this.cflp.getNumFacilities();
        int m = this.cflp.getNumCustomers();

        KeyValue[] facilityDistances;
        
        //=============== Variablen - END ==//

        // Idee:
        //
        // Fuer jeden Customer wird ein neues KeyValue Objekt erstellt
        // Dieses KeyValue Objekt enthaelt die Nummer der Facility und die Distanzkosten
        // Somit bleibt die Uebersicht erhalten und braucht kein Array mit 3 Dimensionen

        // Fuer jeden Customer
        for ( int j = 0; j < m; j++ ){

            // wird ein KeyValue Objekt erstellt
            facilityDistances = new KeyValue[cflp.getNumFacilities()];

            // Hier setze die Distanzkosten fuer alle Facilities die Distanzkosten und die Nummer ein
            for (int i = 0; i < n; i++){
                facilityDistances[i] = new KeyValue(i, cflp.distance(i, j));
            }

            // Sortiere nach Value
            Arrays.sort(facilityDistances);

            // KeyValue Objekte sind sortiert und am Ende in die Liste hinzufuegen
            this.shortestCustomerToFacility.add(facilityDistances);
        }
    }


    /**
     * Liefert nach Greedy-Methode die Facility Nummer mit der kuerzesten Distanz
     *
     * @return - int array mit Facility Nummern
     */
    private int[] initialFill(){

        //=============== Variablen - START ==//

        int m = this.cflp.getNumCustomers();
        int[] customerToFacility = new int[cflp.getNumCustomers()];

        int closestFacility;
        KeyValue[] distance;

        //=============== Variablen - END ==//
        

        // Idee:
        //
        // Gehe jeden Kunden durch und speichere die kuerzeste Distanz in ein
        // Array

        for ( int j = 0; j < m; j++ ) {

            distance = this.shortestCustomerToFacility.get(j);
            closestFacility = distance[0].key;
            customerToFacility[j] = closestFacility;

        }

        return customerToFacility;
    }


    /**
     * Diese Methode bekommt vom Framework maximal 30 Sekunden Zeit zur
     * Verf&uuml;gung gestellt um eine g&uuml;ltige L&ouml;sung
     * zu finden.
     * <p>
     * <p>
     * F&uuml;gen Sie hier Ihre Implementierung des Branch-and-Bound-Algorithmus
     * ein.
     * </p>
     */
    @Override
    public void run() {

        int[] customerAllocation = new int[cflp.getNumCustomers()];

        Arrays.fill(customerAllocation, 0);
        Arrays.fill(this.facilityFixed, false);

        setSolution(this.upperBound, this.greedyAllocation);

        branchAndBound(0, customerAllocation);

    }


    private boolean branchAndBound(int currentCustomer, int[] customerAllocation) {

        //=============== Variablen - START ==//

        int n = this.cflp.getNumFacilities();   // Facility Anzahl
        int m = this.cflp.getNumCustomers();    // Kunden Anzahl

        KeyValue[] shortestDist;

        //=============== Variablen - END ==//
        
        // Rekursionsabbruchbedingung
        // Wenn am letzte Kunde angekommen ist, wird zurueckgegangen
        if (currentCustomer == this.cflp.getNumCustomers()){
            return true;
        }

        // Die kuerzeste Distanz vom derzeitigen Kunden
        shortestDist = this.shortestCustomerToFacility.get(currentCustomer);


        for( int i = 0; i < n; i++ ) {

            int nearestFac = shortestDist[i].key;

            // Fixiere Kunde
            customerAllocation[currentCustomer] = nearestFac;

            // Alle anderen Kunden werden mithilfe von Greedy-Methode den anderen Facilities zugewiesen
            int[] greedyCustomerToFacility = greedyCustomerToFacility(customerAllocation, currentCustomer);

            // Am Anfang ist noch keine Facility fixiert
            // bzw es wird resettet
            Arrays.fill(this.facilityFixed, false);

            // Berechne eine moegliche LowerBound fuer die derzeitige Kunden Belgung
            int lowerBound = calculateLowerBound(greedyCustomerToFacility, currentCustomer);

            if (lowerBound < this.upperBound) {

                // Berechne die UpperBound von den belegten Kunden
                int localUpperBound =  calculateUpperBound(greedyCustomerToFacility);

                if (localUpperBound < this.upperBound) {

                    this.upperBound = localUpperBound;
                    this.greedyAllocation = greedyCustomerToFacility;
                    setSolution(calculateUpperBound(this.greedyAllocation), this.greedyAllocation);

                }

                if (lowerBound < this.upperBound) {
                    branchAndBound(currentCustomer + 1, customerAllocation);
                }
            }
        }
        return true;
    }


    /**
     * Diese Hilfsmethode werden fuer lower und upperBound benoetigt.
     * Hierbei werden den restlichen nicht fixierten Customer eine facility zugwiesen
     * welche die naehste ist (Greedy Style)
     *
     * @param currSetting
     * @param currentCustomer
     * @return
     */
    private int[] greedyCustomerToFacility(int[] currSetting, int currentCustomer){

        //=============== Variablen - START ==//

        int n = this.cflp.getNumFacilities();   // Facility Anzahl
        int m = this.cflp.getNumCustomers();    // Kunden Anzahl

        int[] localSetting = currSetting.clone();

        KeyValue[] distance;
        int nearestFac;

        //=============== Variablen - END ==//

        // Fuellt fuer alle nicht fixierten Customer die naehste Facility auf
        for( int j = currentCustomer + 1; j < m; j++ ) {
            distance = this.shortestCustomerToFacility.get(j);
            nearestFac = distance[0].key;
            localSetting[j] = nearestFac;
        }
        return localSetting;

    }


    /**
     * Die UpperBound weist die nicht fixierten Kunden eine Facility anhand der kuerzesten Distanz
     * zu.
     *
     * @param setting
     * @return
     */
    private int calculateUpperBound(int[] setting) {

        //=============== Variablen - START ==//

        int kosten = 0;

        int bandwidthGebrauch[] = new int[this.cflp.getNumFacilities()];
        int distanzGebrauch[] = new int[this.cflp.getNumFacilities()];

        //=============== Variablen - END ==//


        // Berechne Bandbreite Bedarf fuer jede Facility
        for( int j = 0; j < this.cflp.getNumCustomers(); j++ ) {

            int i = setting[j];                                     // Facility Nr.
            int bandwidthVonCustomer = this.cflp.bandwidths[j];     // Bandwidth Anforderung von Customer

            bandwidthGebrauch[i] += bandwidthVonCustomer;           // Summiere Bandbreitenanforderung
            distanzGebrauch[i] += this.cflp.distance(i, j);
        }

        // Berechne Errichtungskosten
        for( int i = 0; i < this.cflp.getNumFacilities(); i++ ) {

            if( bandwidthGebrauch[i] == 0 ) {
                continue;   // Facility wird nicht gebaut
            }

            int ausbaustufe = (int) Math.ceil(bandwidthGebrauch[i] / (double) this.cflp.maxBandwidths[i]);
            int ausbaustufeKosten = myFactor(ausbaustufe, this.cflp.openingCosts[i]);

            // Berechne Kosten zum Bauen aller Facilities
            kosten += ausbaustufeKosten;

            // Distanzkosten
            kosten += distanzGebrauch[i] * this.cflp.distanceCosts;
        }

        return kosten;
    }


    /**
     * Berechnet die LowerBound fuer die jeweilige Situation
     *
     * Die Idee ist eine "nicht" realistische Kosten der Kunden zu finden.
     * Hierbei werden nur die Distanzkosten und nicht die Errichtungskosten addiert.
     *
     * Falls eine Facility fixiert ist, dann und nur dann werden die Errichtungskosten addiert
     *
     * @param setting
     * @param fixedCustomer
     * @return
     */
    private int calculateLowerBound(int[] setting, int fixedCustomer){

        //=============== Variablen - START ==//

        int kosten = 0;

        int n = this.cflp.getNumFacilities();   // Facility Anzahl
        int m = this.cflp.getNumCustomers();    // Kunden Anzahl

        int e = this.cflp.distanceCosts;        // fixierte Distanzkosten

        int bandwidthGebrauch[] = new int[n];
        int distanzGebrauch[] = new int[n];

        //=============== Variablen - END ==//


        // Idee:
        //
        // Fuer jeden Customer der fixiert ist, wird der Facility status auf true gesetzt
        // Der Facility status hat den Sinn, dass das in der lowerBound die Errichtungskosten
        // fuer die Facility dazugerechnet werden
        // Fuer nicht fixierte Kunden werden keine Errichtungskosten addiert

        for (int c = 0; c <= fixedCustomer; c++){

            int i = setting[c];

            int usedFacility = setting[c];
            this.facilityFixed[usedFacility] = true;

            int bandwidthVonCustomer = this.cflp.bandwidths[c];     // Bandwidth Anforderung von Customer
            bandwidthGebrauch[i] += bandwidthVonCustomer;           // Summiere Bandbreitenanforderung

        }

        // Berechne Bandbreite Bedarf fuer jede Facility
        for( int j = 0; j < m; j++ ) {
            int i = setting[j];
            distanzGebrauch[i] += this.cflp.distance(i, j);
        }

        // Berechne Errichtungskosten
        for( int i = 0; i < n; i++ ) {

            if( bandwidthGebrauch[i] == 0 ) {
                continue;   // Facility wird nicht gebaut
            }

            // Wenn der Status der Facility auf true ist, dann werden die Errichtungskosten/Ausbaustufen
            // dazugerechnet.
            if( this.facilityFixed[i] ) {
                int ausbaustufe = (int) Math.ceil(bandwidthGebrauch[i] / (double) this.cflp.maxBandwidths[i]);
                int ausbaustufeKosten = myFactor(ausbaustufe, this.cflp.openingCosts[i]);

                // Berechne Kosten zum Bauen der fixed Facility
                kosten += ausbaustufeKosten;
            }

            // Distanzkosten zur Facility
            kosten += distanzGebrauch[i] * e;
        }

        return kosten ;
    }


    private int myFactor(int k, int baseCosts) {
        switch (k) {
            case 0:
                return 0;
            case 1:
                return baseCosts;
            case 2:
                return (int) Math.ceil(1.5 * baseCosts);
            default:
                int fib1 = baseCosts;
                int fib2 = (int) Math.ceil(1.5 * baseCosts);
                for (int i = 3; i <= k; i++) {
                    int tmp = fib1 + fib2 + (4 - i) * baseCosts;
                    fib1 = fib2;
                    fib2 = tmp;
                }
                return fib2;
        }
    }

    // Diese Klasse speichert Facility Nummer und die Kosten kompakt ab
    class KeyValue implements Comparable<KeyValue>{
        int key;
        int value;

        private KeyValue(int key, int value){
            this.key = key;
            this.value = value;
        }

        @Override
        public int compareTo(KeyValue o) {
            return Integer.compare(value, o.value);
        }
    }

}