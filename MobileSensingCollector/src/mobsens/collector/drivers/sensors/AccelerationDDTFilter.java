package mobsens.collector.drivers.sensors;

import java.util.Date;

import mobsens.collector.pipeline.BasicGenerator;
import mobsens.collector.pipeline.Consumer;

public class AccelerationDDTFilter extends BasicGenerator<SensorOutput> implements Consumer<SensorOutput>
{
	private Date lastTime;

	private float[] lastValues;

	private final double maxDDT;

	public AccelerationDDTFilter(double maxDDT)
	{
		this.maxDDT = maxDDT;
	}

	@Override
	public void consume(SensorOutput item)
	{
		if (!hasConsumer()) return;

		try
		{
			if (lastTime != null)
			{
				final double dt = (item.time.getTime() - lastTime.getTime()) / 1000.0;
				boolean success = true;

				for (int i = 0; i < Math.min(item.values.length, lastValues.length); i++)
				{
					final float dx = item.values[i] - lastValues[i];

					if (Math.abs(dx / dt) > maxDDT)
					{
						success = false;
						break;
					}
				}

				if (success)
				{
					offer(item);
				}
			}
		}
		finally
		{
			lastTime = item.time;
			lastValues = item.values;
		}
	}
}
