package MobileSensors.Classifiers;

import java.util.ArrayList;

import MobileSensors.Storage.Event.Event;
import MobileSensors.Storage.Sensors.Location;

public class DetectBreaking implements EventClassifier {

	private ArrayList<Location> locations;
	private ArrayList<Event> events;

	public DetectBreaking(ArrayList<Location> locations) {
		this.locations = locations;
		this.events = new ArrayList<Event>();
	}

	@Override
	public ArrayList<Event> getEvents() {

		for (int i = 0; i < locations.size() - 1; i++) {
			Location location = locations.get(i);
			Location nextLocation = locations.get(i + 1);

			boolean breaking = false;
			if (location.getSpeed() / nextLocation.getSpeed() > 1.5) {
				breaking = true;
				i++;
			} else if (i < locations.size() - 2) {
				Location locAfterNextLoc = locations.get(i + 2);

				if (location.getSpeed() / locAfterNextLoc.getSpeed() > 1.75) {
					breaking = true;
					i+=2;
				}
			}

			if (breaking) {
				// System.out.println("breaking: "+location.getSpeed()+" : "+nextlocation.getSpeed());
				this.events.add(new Event(location.getTime(),
						MobileSensors.Storage.Event.EventType.BRAKING));
			}

			// nachfolgende locations mit getSpeed==0 ueberspringen. Nur ein
			// Event fuer eine "Stand-Phase"
			while (locations.get(i++).getSpeed() <= 0.2) {
				if (i == locations.size() - 1)
					break;
			}
			i--;
		}
		return this.events;
	}
}
