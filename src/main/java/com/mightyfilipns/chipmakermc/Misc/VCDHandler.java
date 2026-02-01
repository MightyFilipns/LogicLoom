package com.mightyfilipns.chipmakermc.Misc;

import com.mightyfilipns.chipmakermc.JsonLoader.JsonDesign;
import com.mightyfilipns.chipmakermc.Placment.Placer;
import com.mightyfilipns.chipmakermc.Routing.HyperGraphNet;
import com.mightyfilipns.chipmakermc.Routing.Router;
import com.mightyfilipns.chipmakermc.Routing.TwoPinNet;
import net.minecraft.block.Blocks;
import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class VCDHandler
{
    static Map<Integer, String> id_netname = new HashMap<>();
    static Map<String, Integer> netname_toid = new HashMap<>();
    static Map<String, Boolean> g_valuemap = new HashMap<>();
    static Map<Integer, BlockPos> netid_toout_pos = null;

    public static void GetCurrentValuesAndCompare(ServerWorld w)
    {
        boolean found = false;
        for (HyperGraphNet hp : Router.cached_hy)
        {
            var outpos = hp.all_points.get(hp.allpoints_pos).withY(Placer.last_pos.getY());
            var outpwr = CheckPower(w, outpos);
            for (BlockPos allPoint : hp.pin_port_pos)
            {
                BlockPos inpos = allPoint.withY(Placer.last_pos.getY());
                var pointpwr = CheckPower(w, inpos);
                if (pointpwr != outpwr)
                {
                    found = true;
                    System.out.printf("Bad wire Power out: %s, %s Power in: %s, %s%n", outpos, outpwr, inpos, pointpwr);
                }
            }
        }
        for (TwoPinNet hp : Router.cached_tpn)
        {
            var outpos = hp.p1dir == JsonDesign.PortDirection.Output ? hp.p1 : hp.p2;
            var inpos = hp.p1dir == JsonDesign.PortDirection.Output ? hp.p2 : hp.p1;
            var pwr1 = CheckPower(w, outpos.withY(Placer.last_pos.getY()));
            var pwr2 = CheckPower(w, inpos.withY(Placer.last_pos.getY()));

            if (pwr1 != pwr2)
            {
                found = true;
                System.out.printf("Bad wire Power out: %s, %s Power in: %s, %s%n", outpos, pwr1, inpos, pwr2);
            }
        }
        if (!found)
        {
            System.out.println("No bad wires found");
        }
/*
        Map<String, Boolean> valuemap = new HashMap<>();
        netid_toout_pos = new HashMap<>();
        for (HyperGraphNet hyperGraphNet : Router.cached_hy)
        {
            var outpos = hyperGraphNet.all_points.get(hyperGraphNet.allpoints_pos).withY(Placer.last_pos.getY());
            var isext = w.isReceivingRedstonePower(outpos);
            valuemap.put(id_netname.get(hyperGraphNet.net_id), isext);
            netid_toout_pos.put(hyperGraphNet.net_id, outpos);
        }
        for (TwoPinNet tpn : Router.cached_tpn)
        {
            var outpos = tpn.p1dir == JsonDesign.PortDirection.Output ? tpn.p1.withY(Placer.last_pos.getY()) : tpn.p2.withY(Placer.last_pos.getY());
            var isext = w.isReceivingRedstonePower(outpos);
            valuemap.put(id_netname.get(tpn.id), isext);
            netid_toout_pos.put(tpn.id, outpos);
        }
        boolean found = false;
        for (Map.Entry<String, Boolean> en : valuemap.entrySet())
        {
            if (g_valuemap.get(en.getKey()) != en.getValue())
            {
                found = true;
                System.out.println("Mismatch in at: " + netid_toout_pos.get(netname_toid.get(en.getKey())) + " E: " + g_valuemap.get(en.getKey()) + " F: " + en.getValue());
            }
        }
        if (!found)
        {
            System.out.println("No mismatches found");
        }*/
    }

    private static boolean CheckPower(ServerWorld w, BlockPos outpos)
    {
        if (w.getBlockState(outpos).getBlock() == Blocks.REDSTONE_WIRE)
        {
            return w.getBlockState(outpos).get(RedstoneWireBlock.POWER) != 0;
        }
        return w.isReceivingRedstonePower(outpos);
    }

    public static void SetMap(JsonDesign.DesignModule m)
    {
        for (Map.Entry<String, JsonDesign.JsonWire> en : m.netnames.entrySet())
        {
            if (en.getValue().bits.size() == 1)
            {
                id_netname.put(en.getValue().bits.getFirst(), en.getKey());
                netname_toid.put(en.getKey(), en.getValue().bits.getFirst());
            }
            else
            {
                int i = 0;
                for (Integer bit : en.getValue().bits)
                {
                    id_netname.put(bit, en.getKey() + i);
                    netname_toid.put(en.getKey() + i, bit);
                    i++;
                }
            }
        }
    }

    public static void LoadVCD(String s)
    {
        Map<String, String> namemap = new HashMap<>();
        Map<String, Boolean> valuemap = new HashMap<>();
        List<String> d;
        try {
            d = Files.readAllLines(Path.of(s));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        int i1 = d.indexOf("  $scope module SimpleCalc $end");
        i1++;
        while(!Objects.equals(d.get(i1), "  $upscope $end"))
        {
            var sp = d.get(i1).trim().split(" ");
            int size = Integer.parseInt(sp[2]);
            String vcdname = sp[3];
            String realname = sp[4];
            namemap.put(vcdname, realname);
            i1++;
        }
        int datastart = d.indexOf("#0");
        for (int i = datastart; i < d.size(); i++)
        {
            String cl = d.get(i);
            if (cl.charAt(0) == 'b')
            {
                var vl = cl.split(" ");
                String vcdname = vl[1];
                String v = vl[0].substring(1);
                int i3 = v.length() - 1;
                for (int i2 : v.chars().toArray())
                {
                    valuemap.put(namemap.get(vcdname) + i3, i2 == '1');
                    i3--;
                }
            }
            else
            {
                boolean v = cl.charAt(0) == '1';
                String vcddump = cl.substring(1);
                String nm = namemap.get(vcddump);
                valuemap.put(nm, v);
            }
        }
        g_valuemap = valuemap;
    }
}
