<#macro content shapeModel>

     <#if shapeModel.shapeName == "IpPermission">
    /**
     * <p>
     * One or more IP ranges.
     * </p>
     *
     * @return One or more IP ranges.
     * @deprecated Use {@link #getIpv4Ranges()}.
     */
    @Deprecated
    public java.util.List<String> getIpRanges() {
        if (ipv4Ranges == null) {
            ipv4Ranges = new software.amazon.awssdk.internal.SdkInternalList<IpRange>();
        }
        return newLegacyIpRangeList(ipv4Ranges);
    }

    /**
     * <p>
     * One or more IP ranges.
     * </p>
     *
     * @param ipRanges
     *        One or more IP ranges.
     * @deprecated Use {@link #setIpv4Ranges(java.util.Collection)}
     */
    @Deprecated
    public void setIpRanges(java.util.Collection<String> ipRanges) {
        if (ipRanges == null) {
            this.ipv4Ranges = null;
            return;
        }

        this.ipv4Ranges = newIpRangeList(ipRanges);
    }

    /**
     * <p>
     * One or more IP ranges.
     * </p>
     * <p>
     * <b>NOTE:</b> This method appends the values to the existing list (if any). Use
     * {@link #setIpRanges(java.util.Collection)} or {@link #withIpRanges(java.util.Collection)} if you want to override
     * the existing values.
     * </p>
     *
     * @param ipRanges
     *        One or more IP ranges.
     * @return Returns a reference to this object so that method calls can be chained together.
     * @deprecated Use {@link #withIpv4Ranges(IpRange...)}
     */
    @Deprecated
    public IpPermission withIpRanges(String... ipRanges) {
        if (this.ipv4Ranges == null) {
            setIpRanges(new software.amazon.awssdk.internal.SdkInternalList<String>(ipRanges.length));
        }
        for (String ele : ipRanges) {
            this.ipv4Ranges.add(newIpRange(ele));
        }
        return this;
    }

    /**
     * <p>
     * One or more IP ranges.
     * </p>
     *
     * @param ipRanges
     *        One or more IP ranges.
     * @return Returns a reference to this object so that method calls can be chained together.
     * @deprecated Use {@link #withIpv4Ranges(java.util.Collection)}
     */
    @Deprecated
    public IpPermission withIpRanges(java.util.Collection<String> ipRanges) {
        setIpRanges(ipRanges);
        return this;
    }

    private IpRange newIpRange(String ipRange) {
        return new IpRange().withCidrIp(ipRange);
    }

    private software.amazon.awssdk.internal.SdkInternalList<IpRange> newIpRangeList(java.util.Collection<String> ipRanges) {
        software.amazon.awssdk.internal.SdkInternalList<IpRange> ipRangeList = new software.amazon.awssdk.internal.SdkInternalList<IpRange>(ipRanges.size());
        for (String ipRange : ipRanges) {
            ipRangeList.add(newIpRange(ipRange));
        }
        return ipRangeList;
    }

    private java.util.List<String> newLegacyIpRangeList(java.util.List<IpRange> ipRanges) {
        java.util.List<String> ipRangeList = new java.util.ArrayList<String>();
        for (IpRange ipRange : ipRanges) {
            ipRangeList.add(ipRange.getCidrIp());
        }
        return ipRangeList;
    }
     </#if>
 </#macro>