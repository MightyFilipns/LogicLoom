package com.mightyfilipns.chipmakermc.Routing;

import com.mightyfilipns.chipmakermc.JsonLoader.PortDirection;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

public class ObstacleMap
{
    // TODO: use byte instead of int
    private final List<HashMap<Pair<Integer, Integer>, Integer>> map;

    private final List<HashSet<Pair<Integer, Integer>>> wire_map;

    private HashSet<Pair<Integer, Integer>> exlcd = null;
    private HashSet<Pair<Integer, Integer>> full_exlcd = null;

    private final HashSet<Pair<Integer, Integer>> temproute = new HashSet<>();

    public ObstacleMap(List<HyperGraphNet> hy, List<TwoPinNet> tpn)
    {
        map = new ArrayList<>();
        wire_map = new ArrayList<>();
        map.add(new HashMap<>());

        for (TwoPinNet tp : tpn)
        {
            addAll(Misc.MakeObstMapFromPort(tp.p1, tp.p1dir));
            addAll(Misc.MakeObstMapFromPort(tp.p2, tp.p1dir.Invert()));
        }

        for (HyperGraphNet hyperGraphNet : hy)
        {
            for (int i = 0; i < hyperGraphNet.pin_port_pos.size(); i++)
            {
                var pinPortPo = hyperGraphNet.pin_port_pos.get(i);
                PortDirection dir = i == hyperGraphNet.out_port_pos ? PortDirection.Output : PortDirection.Input;
                addAll(Misc.MakeObstMapFromPort(pinPortPo, dir));
            }
        }
    }

    public int GetMaxY()
    {
        return map.size();
    }

    public void TempExclude(List<HashSet<Pair<Integer, Integer>>> tr)
    {
        exlcd = new HashSet<>();
        for (HashSet<Pair<Integer, Integer>> pairs : tr)
        {
            exlcd.addAll(pairs);
        }
    }

    public void TempAddWire(Pair<Integer, Integer> p)
    {
        temproute.add(p);
    }

    public void CommitWire(int y)
    {
        wire_map.get(y).addAll(temproute);
        temproute.clear();
    }

    public void FlushTemp()
    {
        temproute.clear();
    }

    public void FullRemove(int y)
    {
        var tr = full_exlcd;
        for (int i = y; i < map.size(); i++)
        {
            var cm = map.get(i);
            for (Pair<Integer, Integer> pr : tr)
            {
                cm.compute(pr, (k, v) -> Math.clamp(v - 1, 0, Integer.MAX_VALUE));
            }
        }
    }

    public void AssureY(int y)
    {
        for (int i = wire_map.size(); i <= y; i++)
        {
            wire_map.add(new HashSet<>());
        }
        var m = map.getLast();
        for (int i = map.size(); i <= y; i++)
        {
            map.add((HashMap<Pair<Integer, Integer>, Integer>) m.clone());
        }
    }

    public void addAll(HashSet<Pair<Integer, Integer>> pts)
    {
        for (Pair<Integer, Integer> pt : pts)
        {
            add(pt);
        }
    }

    public void add(Pair<Integer, Integer> p)
    {
        var d = map.getFirst().computeIfAbsent(p, a -> 0);
        d++;
        map.getFirst().put(p, d);
    }

    public boolean IsFree(Pair<Integer, Integer> p, int y)
    {
        var reduction = exlcd.contains(p) ? 1 : 0;
        var tcmp = map.get(y).getOrDefault(p, 0) - reduction;
        if(tcmp < 0)
            throw new RuntimeException("IsFree: tcmp < 0");
        return tcmp == 0;
    }

    public boolean IsFreeIncludeWire(Pair<Integer, Integer> p, int y)
    {
        var reduction = exlcd.contains(p) ? 1 : 0;
        return map.get(y).getOrDefault(p, 0) - reduction == 0 && !wire_map.get(y).contains(p);
    }

    public void SetFullExclude(List<HashSet<Pair<Integer, Integer>>> tr)
    {
        full_exlcd = new HashSet<>();
        for (HashSet<Pair<Integer, Integer>> pairs : tr)
        {
            full_exlcd.addAll(pairs);
        }
    }
}
